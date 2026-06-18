package grupo5.incentivos.models.entities.donante;

import lombok.Getter;

import java.time.LocalDate;
import java.time.ZoneId;

@Getter
public class CambioCategoria {

  private final CategoriaDonante anterior;
  private final CategoriaDonante nueva;
  private final LocalDate fecha;

  public CambioCategoria(CategoriaDonante anterior, CategoriaDonante nueva) {
    this.anterior = anterior;
    this.nueva = nueva;
    this.fecha = LocalDate.now(ZoneId.systemDefault());
  }
}
