package grupo5.incentivos.models.entities.donante;

import java.time.LocalDate;
import java.time.ZoneId;
import lombok.Getter;

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

  private CambioCategoria(CategoriaDonante anterior, CategoriaDonante nueva, LocalDate fecha) {
    this.anterior = anterior;
    this.nueva = nueva;
    this.fecha = fecha;
  }

  /** Reconstituye un cambio ya ocurrido (p. ej. desde la base) conservando su fecha original. */
  public static CambioCategoria reconstituir(
      CategoriaDonante anterior, CategoriaDonante nueva, LocalDate fecha) {
    return new CambioCategoria(anterior, nueva, fecha);
  }
}
