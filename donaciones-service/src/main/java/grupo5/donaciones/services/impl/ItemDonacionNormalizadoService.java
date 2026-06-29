package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.itemsNormalizados.inputs.ItemDonacionNormalizadoPatchDTO;
import grupo5.donaciones.dto.itemsNormalizados.outputs.ItemDonacionNormalizadoOutputDTO;
import grupo5.donaciones.infrastructure.events.DonacionNormalizadaEvent;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.itemsNormalizados.BienNormalizado;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IItemDonacionNormalizadoRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
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
  private final IDonacionesRepository donacionRepository;
  private final ISubcategoriasRepository subcategoriasRepository;
  private final ICategoriasRepository categoriasRepository;
  private final ItemDonacionNormalizadoMapper mapper;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  public List<ItemDonacionNormalizadoOutputDTO> obtenerPendientes() {
    log.info("Obteniendo todos los ítems de donación normalizados pendientes de revisión");
    return itemNormalizadoRepository.findAll().stream()
        .filter(
            item ->
                item.getBien() != null
                    && item.getBien().estadoNormalizacion()
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

    BienNormalizado original = item.getBien();

    if (dto.estadoNormalizacion() == EstadoNormalizacion.RECHAZADO
        && dto.subcategoriaId() != null) {
      // Reclasificación manual al rechazar
      Subcategoria subcategoria =
          subcategoriasRepository
              .findById(dto.subcategoriaId())
              .orElseThrow(() -> new RecursoNoEncontradoException(dto.subcategoriaId()));

      Categoria categoria =
          subcategoria.getCategoriaId() != null
              ? categoriasRepository.findById(subcategoria.getCategoriaId()).orElse(null)
              : null;
      boolean conVencimiento =
          categoria != null && Boolean.TRUE.equals(categoria.getConVencimiento());
      boolean conEstado = categoria != null && Boolean.TRUE.equals(categoria.getConUso());

      BienNormalizado nuevoBien =
          new BienNormalizado(
              original.bienOriginal(),
              subcategoria.getId(),
              1.0,
              EstadoNormalizacion.ACEPTADO,
              conVencimiento,
              conEstado);
      item.actualizarBien(nuevoBien);
      log.info(
          "Reclasificación manual al rechazar: Subcategoría {} asignada. Estado cambia a ACEPTADO.",
          subcategoria.getNombre());
    } else {
      // Modificación estándar (Aceptar con/sin cambio de subcategoría, o Rechazo simple)
      UUID subId = dto.subcategoriaId() != null ? dto.subcategoriaId() : original.subcategoriaId();
      Subcategoria subcategoria =
          subcategoriasRepository
              .findById(subId)
              .orElseThrow(() -> new RecursoNoEncontradoException(subId));

      Categoria categoria =
          subcategoria.getCategoriaId() != null
              ? categoriasRepository.findById(subcategoria.getCategoriaId()).orElse(null)
              : null;
      boolean conVencimiento =
          categoria != null && Boolean.TRUE.equals(categoria.getConVencimiento());
      boolean conEstado = categoria != null && Boolean.TRUE.equals(categoria.getConUso());

      double conf = dto.subcategoriaId() != null ? 1.0 : original.confianza();

      BienNormalizado nuevoBien =
          new BienNormalizado(
              original.bienOriginal(),
              subId,
              conf,
              dto.estadoNormalizacion(),
              conVencimiento,
              conEstado);
      item.actualizarBien(nuevoBien);
      if (dto.subcategoriaId() != null) {
        log.info(
            "Categorización manual provista. Subcategoría {} asignada.", subcategoria.getNombre());
      }
    }

    itemNormalizadoRepository.save(item);

    // Verificar el estado de la Donación original
    if (item.getDonacionOriginalId() != null) {
      UUID donacionId = item.getDonacionOriginalId();
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
                      i.getDonacionOriginalId() != null
                          && i.getDonacionOriginalId().equals(donacionId))
              .toList();

      boolean tienePendientes =
          itemsDeDonacion.stream()
              .anyMatch(
                  i -> i.getBien().estadoNormalizacion() == EstadoNormalizacion.PENDIENTE_REVISION);

      if (!tienePendientes) {
        donacion.marcarNormalizada();
        donacionRepository.save(donacion);
        log.info(
            "Todos los ítems de la donación {} fueron revisados. Donación cambia a estado NORMALIZADA y se emite evento.",
            donacionId);
        eventPublisher.publishEvent(new DonacionNormalizadaEvent(donacionId));
      }
    }

    return mapper.toOutputDTO(item);
  }

  public ItemDonacionNormalizadoOutputDTO obtener(UUID id) {
    log.info("Obteniendo ítem de donación normalizado por ID: {}", id);
    ItemDonacionNormalizado item =
        itemNormalizadoRepository
            .findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(id));
    return mapper.toOutputDTO(item);
  }
}
