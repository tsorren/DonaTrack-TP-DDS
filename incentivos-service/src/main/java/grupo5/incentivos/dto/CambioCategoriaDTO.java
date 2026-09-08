package grupo5.incentivos.dto;

import grupo5.incentivos.models.entities.donante.CambioCategoria;
import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import java.time.LocalDate;

public record CambioCategoriaDTO(
    CategoriaDonante categoriaAnterior, CategoriaDonante categoriaNueva, LocalDate fecha) {

  public static CambioCategoriaDTO desde(CambioCategoria cambio) {
    return new CambioCategoriaDTO(cambio.getAnterior(), cambio.getNueva(), cambio.getFecha());
  }
}
