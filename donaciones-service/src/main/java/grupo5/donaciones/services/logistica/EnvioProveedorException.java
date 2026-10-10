package grupo5.donaciones.services.logistica;

import lombok.Getter;

/**
 * Base de las excepciones que lanza un adapter de proveedor de logística. Nunca llegan a un
 * controller: el relay del outbox las traduce a un {@link ResultadoEnvio}.
 */
@Getter
public abstract class EnvioProveedorException extends RuntimeException {

  private final String proveedorId;

  protected EnvioProveedorException(String proveedorId, String motivo, Throwable causa) {
    super("Proveedor " + proveedorId + ": " + motivo, causa);
    this.proveedorId = proveedorId;
  }
}
