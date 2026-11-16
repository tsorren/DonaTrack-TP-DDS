package grupo5.cliente.puertos;

import java.io.Serial;
import java.util.List;

/**
 * Donaciones no aceptó los datos (400): {@code campos} son los del formulario que hay que revisar
 * ({@code entidad}, {@code calle}…), en el orden en que llegaron.
 */
public final class DatosRechazados extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  private final transient List<String> campos;

  public DatosRechazados(List<String> campos) {
    super("Donaciones no aceptó los datos de la solicitud");
    this.campos = List.copyOf(campos);
  }

  public List<String> campos() {
    return campos;
  }
}
