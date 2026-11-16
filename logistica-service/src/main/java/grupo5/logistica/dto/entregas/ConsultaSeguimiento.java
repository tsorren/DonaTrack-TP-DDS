package grupo5.logistica.dto.entregas;

import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import java.util.List;
import java.util.UUID;

/**
 * Lo que se pide del seguimiento (R11-43), tal como llega por la URL: {@code estado} e {@code
 * idDonacion}, repetibles (nulos o vacíos no filtran), y la página. Sin {@code page} ni {@code
 * size}, la página 0 de 20; {@code size} de 1 a 100 y hasta 100 {@code idDonacion}.
 */
public record ConsultaSeguimiento(
    List<EstadoEntrega> estado, List<UUID> idDonacion, Integer page, Integer size) {

  public static final int TAMANIO_POR_DEFECTO = 20;
  public static final int TAMANIO_MAXIMO = 100;

  /** La primera página de 20, sin filtros. */
  public static ConsultaSeguimiento primeraPagina() {
    return new ConsultaSeguimiento(null, null, null, null);
  }
}
