package grupo5.donaciones.services.mappers;

import grupo5.donaciones.dto.donacionesIndependientes.BienNormalizadoDTO;
import grupo5.donaciones.dto.donacionesIndependientes.BienResumenDTO;
import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDIResponseDTO;
import grupo5.donaciones.dto.donacionesIndependientes.CategoriaResumenDTO;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.dto.donacionesIndependientes.ItemDonacionIndependienteResponseDTO;
import grupo5.donaciones.dto.donacionesIndependientes.SubcategoriaResumenDTO;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
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

    List<ItemDonacionIndependienteResponseDTO> itemsMapped =
        donacion.getItems().stream()
            .map(
                item -> {
                  BienNormalizado bienNormalizado = item.bien();
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

                  BienNormalizadoDTO bienDTO =
                      new BienNormalizadoDTO(bienResumen, subcategoriaResumen, categoriaResumen);
                  return new ItemDonacionIndependienteResponseDTO(bienDTO, item.cantidad());
                })
            .toList();

    List<CambioEstadoDIResponseDTO> historialMapped =
        donacion.getHistorial().stream()
            .map(
                c ->
                    new CambioEstadoDIResponseDTO(
                        c.getEstadoAnterior() != null
                            ? c.getEstadoAnterior().getClass().getSimpleName()
                            : null,
                        c.getEstadoNuevo().getClass().getSimpleName(),
                        c.getTimestamp(),
                        c.getJustificacion(),
                        c.getActor()))
            .toList();

    return new DonacionIndependienteResponseDTO(
        donacion.getId(),
        donacion.getDonacionOriginalId(),
        donacion.getDescripcion(),
        donacion.getEstadoActual().getClass().getSimpleName(),
        donacion.getFechaRegistro(),
        historialMapped,
        itemsMapped,
        donacion.getCantidad());
  }

  private BienResumenDTO mapBienResumen(Bien bienOriginal) {
    if (bienOriginal == null) {
      return null;
    }
    return new BienResumenDTO(
        bienOriginal.descripcion(),
        bienOriginal.fotoUrl(),
        bienOriginal.fechaVencimiento(),
        bienOriginal.estado());
  }

  private SubcategoriaResumenDTO mapSubcategoriaResumen(
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
