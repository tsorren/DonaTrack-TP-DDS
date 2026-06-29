package grupo5.donaciones.infrastructure.events;

import grupo5.donaciones.dto.comunicaciones.NuevaDonacionRequest;
import grupo5.donaciones.infrastructure.clients.IncentivosFeignClient;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.ports.Segmentador;
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IItemDonacionNormalizadoRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import grupo5.donaciones.models.repositories.impl.DonacionRepositoryEnMemoria;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SegmentacionEventListener {

  private static final Logger log = LoggerFactory.getLogger(SegmentacionEventListener.class);

  private final IItemDonacionNormalizadoRepository itemNormalizadoRepository;
  private final DonacionRepositoryEnMemoria donacionRepository;
  private final Segmentador segmentador;
  private final IDonacionesIndependientesRepository donacionesIndependientesRepository;
  private final IncentivosFeignClient incentivosFeignClient;
  private final ICategoriasRepository categoriasRepository;
  private final ISubcategoriasRepository subcategoriasRepository;
  private final IPersonasRepository personasRepository;

  @EventListener
  public void onDonacionNormalizada(DonacionNormalizadaEvent event) {
    log.info("Capturando DonacionNormalizadaEvent para donación ID: {}", event.donacionId());

    Donacion donacion =
        donacionRepository
            .findById(event.donacionId())
            .orElseThrow(
                () -> new IllegalStateException("Donación no encontrada: " + event.donacionId()));

    List<ItemDonacionNormalizado> itemsDeDonacion =
        itemNormalizadoRepository.findAll().stream()
            .filter(
                item ->
                    item.getDonacionOriginal() != null
                        && item.getDonacionOriginal().getId().equals(event.donacionId()))
            .toList();

    List<ItemDonacionNormalizado> itemsAceptados =
        itemsDeDonacion.stream()
            .filter(
                item ->
                    item.getBien().estadoNormalizacion() == EstadoNormalizacion.ACEPTADO
                        && !item.isSegmentado())
            .toList();

    if (itemsAceptados.isEmpty()) {
      log.info(
          "No hay ítems aceptados para segmentar en la donación ID: {}. Avanzando estado a SEGMENTADA directamente.",
          event.donacionId());
      donacion.marcarSegmentada();
      donacionRepository.save(donacion);
      return;
    }

    log.info(
        "Segmentando {} ítems aceptados para la donación ID: {}",
        itemsAceptados.size(),
        event.donacionId());
    List<DonacionIndependiente> donacionesIndependientes = segmentador.segmentar(itemsAceptados);

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

    // Registrar en motor de incentivos
    for (DonacionIndependiente di : donacionesIndependientes) {
      List<String> categorias =
          di.getItems().stream()
              .map(
                  item -> {
                    UUID subId = item.getBien().subcategoriaId();
                    UUID catId =
                        subId != null
                            ? subcategoriasRepository
                                .findById(subId)
                                .map(Subcategoria::getCategoriaId)
                                .orElse(null)
                            : null;
                    return catId != null
                        ? categoriasRepository
                            .findById(catId)
                            .map(Categoria::getNombre)
                            .orElse("Desconocida")
                        : "Desconocida";
                  })
              .distinct()
              .toList();

      UUID personaId = di.getDonacionOriginal().getDonante().personaId();
      Persona persona =
          personasRepository
              .findById(personaId)
              .orElseThrow(() -> new IllegalStateException("Persona no encontrada: " + personaId));
      String nombreDonante = obtenerNombrePersona(persona);

      NuevaDonacionRequest request =
          new NuevaDonacionRequest(
              di.getDonacionOriginal().getDonante().getId(),
              categorias,
              di.getCantidad(),
              di.getDonacionOriginal().getFecha().toLocalDate(),
              nombreDonante);

      try {
        log.info(
            "Registrando donación en motor de incentivos para donante ID: {}", request.donanteId());
        incentivosFeignClient.procesarDonacion(request);
      } catch (Exception e) {
        log.error("Error al registrar donación en motor de incentivos: {}", e.getMessage());
      }
    }

    // Persistir las donaciones independientes
    donacionesIndependientesRepository.saveAll(donacionesIndependientes);

    // Marcar los ítems procesados como segmentados
    itemsAceptados.forEach(
        i -> {
          i.setSegmentado(true);
          itemNormalizadoRepository.save(i);
        });

    // Cambiar estado de donación original a SEGMENTADA
    donacion.marcarSegmentada();
    donacionRepository.save(donacion);
    log.info("Donación original ID {} movida a SEGMENTADA.", donacion.getId());
  }

  private String obtenerNombrePersona(Persona persona) {
    return switch (persona) {
      case Humana h -> h.getNombre() + " " + h.getApellido();
      case Juridica j -> j.getRazonSocial();
    };
  }
}
