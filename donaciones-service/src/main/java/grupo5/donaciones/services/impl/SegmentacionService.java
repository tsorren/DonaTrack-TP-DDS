package grupo5.donaciones.services.impl;

import grupo5.donaciones.dto.comunicaciones.EventoDonacionSegmentadaV1;
import grupo5.donaciones.dto.comunicaciones.ItemSegmentadoEventoDTO;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.events.DonacionNormalizada;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.ports.Segmentador;
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IItemDonacionNormalizadoRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import grupo5.donaciones.services.IDonacionesEventPublisher;
import grupo5.donaciones.services.ISegmentacionService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
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
  private final ICategoriasRepository categoriasRepository;
  private final ISubcategoriasRepository subcategoriasRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final IDonacionesEventPublisher donacionesEventPublisher;

  public SegmentacionService(
      IItemDonacionNormalizadoRepository itemNormalizadoRepository,
      IDonacionesRepository donacionRepository,
      Segmentador segmentador,
      IDonacionesIndependientesRepository donacionesIndependientesRepository,
      ICategoriasRepository categoriasRepository,
      ISubcategoriasRepository subcategoriasRepository,
      ApplicationEventPublisher eventPublisher,
      IDonacionesEventPublisher donacionesEventPublisher) {
    this.itemNormalizadoRepository = itemNormalizadoRepository;
    this.donacionRepository = donacionRepository;
    this.segmentador = segmentador;
    this.donacionesIndependientesRepository = donacionesIndependientesRepository;
    this.categoriasRepository = categoriasRepository;
    this.subcategoriasRepository = subcategoriasRepository;
    this.eventPublisher = eventPublisher;
    this.donacionesEventPublisher = donacionesEventPublisher;
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
    List<ItemSegmentadoEventoDTO> itemsSegmentados =
        registrarEnIncentivos(donacionesIndependientes);

    donacionesIndependientesRepository.saveAll(donacionesIndependientes);

    itemsAceptados.forEach(
        i -> {
          i.marcarComoSegmentado();
          itemNormalizadoRepository.save(i);
        });

    marcarSegmentadaYPublicar(donacion);
    publicarDonacionSegmentada(donacion, itemsSegmentados);
    log.info("Donación original ID {} movida a SEGMENTADA.", donacion.getId());
  }

  private void publicarDonacionSegmentada(Donacion donacion, List<ItemSegmentadoEventoDTO> items) {
    if (items.isEmpty()) {
      return;
    }
    try {
      donacionesEventPublisher.publicarDonacionSegmentada(
          new EventoDonacionSegmentadaV1(
              donacion.getDonanteId(),
              donacion.getFecha() != null
                  ? donacion.getFecha()
                  : LocalDateTime.now(ZoneId.systemDefault()),
              items));
    } catch (Exception e) {
      log.error(
          "Error al publicar donacion.segmentada.v1 para donación {}: {}",
          donacion.getId(),
          e.getMessage(),
          e);
    }
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

  private List<ItemSegmentadoEventoDTO> registrarEnIncentivos(
      List<DonacionIndependiente> donacionesIndependientes) {
    List<ItemSegmentadoEventoDTO> items = new ArrayList<>();
    for (DonacionIndependiente di : donacionesIndependientes) {
      List<String> categorias = obtenerCategoriasDeItems(di);
      if (categorias.isEmpty()) {
        log.warn(
            "Ítem segmentado {} sin categoría resuelta, no se incluye en donacion.segmentada.v1",
            di.getId());
        continue;
      }
      String categoria = categorias.get(0);

      log.info(
          "Ítem segmentado para incentivos: Categoría {}, Cantidad {}",
          categoria,
          di.getCantidad());

      items.add(new ItemSegmentadoEventoDTO(categoria, di.getCantidad()));
    }
    return items;
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
