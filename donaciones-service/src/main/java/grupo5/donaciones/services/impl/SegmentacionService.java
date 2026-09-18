package grupo5.donaciones.services.impl;

import grupo5.donaciones.dto.comunicaciones.NuevaDonacionRequest;
import grupo5.donaciones.infrastructure.clients.IncentivosFeignClient;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.events.DonacionNormalizada;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.ports.Segmentador;
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.models.repositories.IItemDonacionNormalizadoRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import grupo5.donaciones.services.ISegmentacionService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class SegmentacionService implements ISegmentacionService {

  private static final Logger log = LoggerFactory.getLogger(SegmentacionService.class);

  private final IItemDonacionNormalizadoRepository itemNormalizadoRepository;
  private final IDonacionesRepository donacionRepository;
  private final Segmentador segmentador;
  private final IDonacionesIndependientesRepository donacionesIndependientesRepository;
  private final IncentivosFeignClient incentivosFeignClient;
  private final ICategoriasRepository categoriasRepository;
  private final ISubcategoriasRepository subcategoriasRepository;
  private final IPersonasRepository personasRepository;
  private final IDonantesRepository donantesRepository;
  private final ApplicationEventPublisher eventPublisher;

  public SegmentacionService(
      IItemDonacionNormalizadoRepository itemNormalizadoRepository,
      IDonacionesRepository donacionRepository,
      Segmentador segmentador,
      IDonacionesIndependientesRepository donacionesIndependientesRepository,
      IncentivosFeignClient incentivosFeignClient,
      ICategoriasRepository categoriasRepository,
      ISubcategoriasRepository subcategoriasRepository,
      IPersonasRepository personasRepository,
      IDonantesRepository donantesRepository,
      ApplicationEventPublisher eventPublisher) {
    this.itemNormalizadoRepository = itemNormalizadoRepository;
    this.donacionRepository = donacionRepository;
    this.segmentador = segmentador;
    this.donacionesIndependientesRepository = donacionesIndependientesRepository;
    this.incentivosFeignClient = incentivosFeignClient;
    this.categoriasRepository = categoriasRepository;
    this.subcategoriasRepository = subcategoriasRepository;
    this.personasRepository = personasRepository;
    this.donantesRepository = donantesRepository;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public void procesarDonacionNormalizada(DonacionNormalizada event) {
    log.info("Procesando DonacionNormalizada para donación ID: {}", event.donacionId());

    Donacion donacion =
        donacionRepository
            .findById(event.donacionId())
            .orElseThrow(
                () -> new IllegalStateException("Donación no encontrada: " + event.donacionId()));

    List<ItemDonacionNormalizado> itemsAceptados =
        obtenerItemsAceptadosNoSegmentados(event.donacionId());

    if (itemsAceptados.isEmpty()) {
      log.info(
          "No hay ítems aceptados para segmentar en la donación ID: {}. Avanzando estado a SEGMENTADA directamente.",
          event.donacionId());
      marcarSegmentadaYPublicar(donacion);
      return;
    }

    log.info(
        "Segmentando {} ítems aceptados para la donación ID: {}",
        itemsAceptados.size(),
        event.donacionId());
    List<DonacionIndependiente> donacionesIndependientes = segmentador.segmentar(itemsAceptados);

    logDonacionesIndependientes(donacionesIndependientes);
    registrarEnIncentivos(donacionesIndependientes);

    donacionesIndependientesRepository.saveAll(donacionesIndependientes);

    itemsAceptados.forEach(
        i -> {
          i.marcarComoSegmentado();
          itemNormalizadoRepository.save(i);
        });

    marcarSegmentadaYPublicar(donacion);
    log.info("Donación original ID {} movida a SEGMENTADA.", donacion.getId());
  }

  private void marcarSegmentadaYPublicar(Donacion donacion) {
    donacion.marcarSegmentada();
    donacionRepository.save(donacion);
    var eventos = donacion.getDomainEvents();
    donacion.clearDomainEvents();
    eventos.forEach(eventPublisher::publishEvent);
  }

  private List<ItemDonacionNormalizado> obtenerItemsAceptadosNoSegmentados(UUID donacionId) {
    return itemNormalizadoRepository.findAll().stream()
        .filter(
            item ->
                item.getDonacionOriginalId() != null
                    && item.getDonacionOriginalId().equals(donacionId))
        .filter(
            item ->
                item.getBien().estadoNormalizacion() == EstadoNormalizacion.ACEPTADO
                    && !item.isSegmentado())
        .toList();
  }

  private void logDonacionesIndependientes(List<DonacionIndependiente> donacionesIndependientes) {
    for (DonacionIndependiente di : donacionesIndependientes) {
      String subcatNombre = "null";
      if (di.getSubcategoriaId() != null) {
        subcatNombre =
            subcategoriasRepository
                .findById(di.getSubcategoriaId())
                .map(Subcategoria::getNombre)
                .orElse("null");
      }
      log.info(
          "  Donación Independiente ID: {}, Subcategoría: {}, Cantidad: {}, Estado: {}",
          di.getId(),
          subcatNombre,
          di.getCantidad(),
          di.getEstadoActual() != null ? di.getEstadoActual().getClass().getSimpleName() : "null");
    }
  }

  private void registrarEnIncentivos(List<DonacionIndependiente> donacionesIndependientes) {
    for (DonacionIndependiente di : donacionesIndependientes) {
      List<String> categorias = obtenerCategoriasDeItems(di);

      UUID donacionOriginalId = di.getDonacionOriginalId();
      Donacion donacionOriginal =
          donacionRepository
              .findById(donacionOriginalId)
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Donación original no encontrada: " + donacionOriginalId));
      UUID donanteId = donacionOriginal.getDonanteId();
      Donante donante =
          donantesRepository
              .findById(donanteId)
              .orElseThrow(() -> new IllegalStateException("Donante no encontrado: " + donanteId));
      UUID personaId = donante.personaId();
      Persona persona =
          personasRepository
              .findById(personaId)
              .orElseThrow(() -> new IllegalStateException("Persona no encontrada: " + personaId));
      String nombreDonante = obtenerNombrePersona(persona);

      log.info(
          "Registrando donación en incentivos: Donante ID {}, Nombre {}, Cantidad {}, Categorías {}",
          donanteId,
          nombreDonante,
          di.getCantidad(),
          categorias);

      try {
        incentivosFeignClient.procesarDonacion(
            new NuevaDonacionRequest(
                donanteId,
                categorias,
                di.getCantidad(),
                donacionOriginal.getFecha() != null
                    ? donacionOriginal.getFecha().toLocalDate()
                    : java.time.LocalDate.now(java.time.ZoneId.systemDefault()),
                nombreDonante));
      } catch (Exception e) {
        log.error("Error al registrar donación en incentivos: {}", e.getMessage(), e);
      }
    }
  }

  private static String obtenerNombrePersona(Persona persona) {
    if (persona instanceof Humana h) {
      return h.getNombre() + " " + h.getApellido();
    } else if (persona instanceof Juridica j) {
      return j.getRazonSocial();
    }
    return "Donante Anónimo";
  }

  private List<String> obtenerCategoriasDeItems(DonacionIndependiente di) {
    if (di.getSubcategoriaId() == null) {
      return List.of();
    }
    return subcategoriasRepository
        .findById(di.getSubcategoriaId())
        .map(Subcategoria::getCategoriaId)
        .flatMap(categoriasRepository::findById)
        .map(Categoria::getNombre)
        .map(List::of)
        .orElse(List.of());
  }
}
