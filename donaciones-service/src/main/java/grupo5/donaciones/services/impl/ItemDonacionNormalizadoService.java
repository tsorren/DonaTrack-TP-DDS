package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.itemsNormalizados.inputs.ItemDonacionNormalizadoPatchDTO;
import grupo5.donaciones.dto.itemsNormalizados.outputs.ItemDonacionNormalizadoOutputDTO;
import grupo5.donaciones.infrastructure.events.DonacionNormalizadaEvent;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.repositories.IItemDonacionNormalizadoRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import grupo5.donaciones.models.repositories.impl.DonacionRepositoryEnMemoria;
import grupo5.donaciones.services.IItemDonacionNormalizadoService;
import grupo5.donaciones.services.mappers.ItemDonacionNormalizadoMapper;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItemDonacionNormalizadoService implements IItemDonacionNormalizadoService {

  private static final Logger log = LoggerFactory.getLogger(ItemDonacionNormalizadoService.class);

  private final IItemDonacionNormalizadoRepository itemNormalizadoRepository;
  private final DonacionRepositoryEnMemoria donacionRepository;
  private final ISubcategoriasRepository subcategoriasRepository;
  private final ItemDonacionNormalizadoMapper mapper;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  public List<ItemDonacionNormalizadoOutputDTO> obtenerPendientes() {
    log.info("Obteniendo todos los ítems de donación normalizados pendientes de revisión");
    return itemNormalizadoRepository.findAll().stream()
        .filter(
            item ->
                item.getBien() != null
                    && item.getBien().getEstadoNormalizacion()
                        == EstadoNormalizacion.PENDIENTE_REVISION)
        .map(mapper::toOutputDTO)
        .toList();
  }

  @Override
  public ItemDonacionNormalizadoOutputDTO actualizarEstado(
      UUID id, ItemDonacionNormalizadoPatchDTO dto) {
    log.info("Actualizando estado de ítem normalizado ID: {} con {}", id, dto);

    ItemDonacionNormalizado item =
        itemNormalizadoRepository
            .findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(id));

    if (dto.estadoNormalizacion() == EstadoNormalizacion.RECHAZADO
        && dto.subcategoriaId() != null) {
      // Reclasificación manual al rechazar
      Subcategoria subcategoria =
          subcategoriasRepository
              .findById(dto.subcategoriaId())
              .orElseThrow(() -> new RecursoNoEncontradoException(dto.subcategoriaId()));
      item.getBien().setSubcategoria(subcategoria);
      item.getBien().setConfianza(1.0);
      item.getBien().setEstadoNormalizacion(EstadoNormalizacion.ACEPTADO);
      log.info(
          "Reclasificación manual al rechazar: Subcategoría {} asignada. Estado cambia a ACEPTADO.",
          subcategoria.getNombre());
    } else {
      // Modificación estándar (Aceptar con/sin cambio de subcategoría, o Rechazo simple)
      item.getBien().setEstadoNormalizacion(dto.estadoNormalizacion());
      if (dto.subcategoriaId() != null) {
        Subcategoria subcategoria =
            subcategoriasRepository
                .findById(dto.subcategoriaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(dto.subcategoriaId()));
        item.getBien().setSubcategoria(subcategoria);
        item.getBien().setConfianza(1.0);
        log.info(
            "Categorización manual provista. Subcategoría {} asignada.", subcategoria.getNombre());
      }
    }

    itemNormalizadoRepository.save(item);

    // Verificar el estado de la Donación original
    if (item.getDonacionOriginal() != null) {
      UUID donacionId = item.getDonacionOriginal().getId();
      Donacion donacion =
          donacionRepository
              .findById(donacionId)
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Donación original no encontrada para el ID: " + donacionId));

      List<ItemDonacionNormalizado> itemsDeDonacion =
          itemNormalizadoRepository.findAll().stream()
              .filter(
                  i ->
                      i.getDonacionOriginal() != null
                          && i.getDonacionOriginal().getId().equals(donacionId))
              .toList();

      boolean tienePendientes =
          itemsDeDonacion.stream()
              .anyMatch(
                  i ->
                      i.getBien().getEstadoNormalizacion()
                          == EstadoNormalizacion.PENDIENTE_REVISION);

      if (!tienePendientes) {
        log.info(
            "Todos los ítems de la donación ID: {} han sido resueltos. Avanzando estado de donación a NORMALIZADA.",
            donacionId);
        donacion.marcarNormalizada();
        donacionRepository.save(donacion);

        // Disparar evento de normalización completada para que comience la segmentación
        log.info("Publicando DonacionNormalizadaEvent para donación ID: {}", donacionId);
        eventPublisher.publishEvent(new DonacionNormalizadaEvent(donacionId));
      }
    }

    return mapper.toOutputDTO(item);
  }
}
