package grupo5.donaciones.services.mappers;

import grupo5.donaciones.dto.donacionesIndependientes.*;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donacionesIndependientes.CambioEstado;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente;
import grupo5.donaciones.models.entities.itemsNormalizados.BienNormalizado;
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DonacionIndependienteMapper {

  private final ISubcategoriasRepository subcategoriasRepository;
  private final ICategoriasRepository categoriasRepository;

  public DonacionIndependienteMapper(
      ISubcategoriasRepository subcategoriasRepository,
      ICategoriasRepository categoriasRepository) {
    this.subcategoriasRepository = subcategoriasRepository;
    this.categoriasRepository = categoriasRepository;
  }

  public DonacionIndependienteResponseDTO toDTO(DonacionIndependiente donacion) {
    if (donacion == null) {
      return null;
    }

    List<ItemDonacionIndependienteResponseDTO> itemsMapped = mapItems(donacion.getItems());
    List<CambioEstadoDIResponseDTO> historialMapped = mapHistorial(donacion.getHistorial());

    return new DonacionIndependienteResponseDTO(
        donacion.getId(),
        donacion.getDonacionOriginalId(),
        donacion.getDescripcion(),
        donacion.getEstadoActual() != null
            ? donacion.getEstadoActual().getClass().getSimpleName()
            : null,
        donacion.getFechaRegistro(),
        historialMapped,
        itemsMapped,
        donacion.getCantidad());
  }

  private List<ItemDonacionIndependienteResponseDTO> mapItems(
      List<ItemDonacionIndependiente> items) {
    if (items == null) {
      return List.of();
    }
    return items.stream().map(this::mapItem).toList();
  }

  private ItemDonacionIndependienteResponseDTO mapItem(ItemDonacionIndependiente item) {
    if (item == null) {
      return null;
    }
    BienNormalizado bienNormalizado = item.bien();
    BienNormalizadoDTO bienDTO = mapBienNormalizado(bienNormalizado);
    return new ItemDonacionIndependienteResponseDTO(bienDTO, item.cantidad());
  }

  private BienNormalizadoDTO mapBienNormalizado(BienNormalizado bienNormalizado) {
    if (bienNormalizado == null) {
      return null;
    }
    Bien bienOriginal = bienNormalizado.bienOriginal();
    BienResumenDTO bienResumen = mapBienResumen(bienOriginal);

    SubcategoriaResumenDTO subcategoriaResumen = null;
    CategoriaResumenDTO categoriaResumen = null;
    if (bienNormalizado.subcategoriaId() != null) {
      var subOpt = subcategoriasRepository.findById(bienNormalizado.subcategoriaId());
      if (subOpt.isPresent()) {
        var sub = subOpt.get();
        subcategoriaResumen = mapSubcategoriaResumen(sub);
        categoriaResumen = mapCategoriaResumen(sub);
      }
    }
    return new BienNormalizadoDTO(bienResumen, subcategoriaResumen, categoriaResumen);
  }

  private static List<CambioEstadoDIResponseDTO> mapHistorial(List<CambioEstado> historial) {
    if (historial == null) {
      return List.of();
    }
    return historial.stream().map(DonacionIndependienteMapper::mapCambioEstado).toList();
  }

  private static CambioEstadoDIResponseDTO mapCambioEstado(CambioEstado c) {
    if (c == null) {
      return null;
    }
    String estadoAnterior =
        c.getEstadoAnterior() != null ? c.getEstadoAnterior().getClass().getSimpleName() : null;
    String estadoNuevo =
        c.getEstadoNuevo() != null ? c.getEstadoNuevo().getClass().getSimpleName() : null;
    return new CambioEstadoDIResponseDTO(
        estadoAnterior, estadoNuevo, c.getTimestamp(), c.getJustificacion(), c.getActor());
  }

  private static BienResumenDTO mapBienResumen(Bien bienOriginal) {
    if (bienOriginal == null) {
      return null;
    }
    return new BienResumenDTO(
        bienOriginal.descripcion(),
        bienOriginal.fotoUrl(),
        bienOriginal.fechaVencimiento(),
        bienOriginal.estado());
  }

  private static SubcategoriaResumenDTO mapSubcategoriaResumen(
      grupo5.donaciones.models.entities.categorias.Subcategoria sub) {
    if (sub == null) {
      return null;
    }
    return new SubcategoriaResumenDTO(sub.getId(), sub.getNombre());
  }

  private CategoriaResumenDTO mapCategoriaResumen(
      grupo5.donaciones.models.entities.categorias.Subcategoria sub) {
    if (sub == null || sub.getCategoriaId() == null) {
      return null;
    }
    return categoriasRepository
        .findById(sub.getCategoriaId())
        .map(
            cat ->
                new CategoriaResumenDTO(
                    cat.getId(),
                    cat.getNombre(),
                    cat.getTipoUnidad() != null ? cat.getTipoUnidad().name() : "UNIDADES"))
        .orElse(null);
  }
}
