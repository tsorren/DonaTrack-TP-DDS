package grupo5.donaciones.services.mappers;

import grupo5.donaciones.dto.donacionesIndependientes.BienNormalizadoDTO;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.dto.donacionesIndependientes.ItemDonacionIndependienteResponseDTO;
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
                  BienNormalizado bien = item.bien();
                  String descripcion =
                      bien.bienOriginal() != null ? bien.bienOriginal().descripcion() : "";
                  String unidadNombre = "UNIDADES";

                  if (bien.subcategoriaId() != null) {
                    unidadNombre =
                        subcategoriasRepository
                            .findById(bien.subcategoriaId())
                            .flatMap(
                                sub -> {
                                  if (sub.getCategoriaId() != null) {
                                    return categoriasRepository
                                        .findById(sub.getCategoriaId())
                                        .map(cat -> cat.getTipoUnidad().name());
                                  }
                                  return java.util.Optional.empty();
                                })
                            .orElse("UNIDADES");
                  }

                  BienNormalizadoDTO bienDTO = new BienNormalizadoDTO(descripcion, unidadNombre);
                  return new ItemDonacionIndependienteResponseDTO(bienDTO, item.cantidad());
                })
            .toList();

    return new DonacionIndependienteResponseDTO(
        donacion.getId(),
        donacion.getEstadoActual().getClass().getSimpleName(),
        donacion.getHistorial().stream()
            .map(c -> c.getEstadoNuevo().getClass().getSimpleName())
            .toList(),
        itemsMapped,
        donacion.getCantidad());
  }
}
