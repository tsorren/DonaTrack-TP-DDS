package grupo5.cliente.puertos;

import grupo5.cliente.vistas.SolicitudEntidadVista;
import java.io.Serial;
import java.util.List;
import java.util.UUID;

/**
 * La solicitud de la organización que espera la aprobación (R11-41), con el token de la cuenta: la
 * lee y la corrige la propia cuenta, que todavía no tiene rol. Si el equipo ya la aprobó, las dos
 * operaciones lanzan {@link YaAprobada}; si Donaciones no acepta los datos, {@link
 * DatosRechazados}. Cualquier otra falla (sin respuesta a tiempo, por ejemplo) sigue su camino.
 */
public interface SolicitudEntidadPuerto {

  SolicitudEntidadVista leer(UUID personaId);

  /** Guarda lo corregido; el correo no se cambia acá. */
  void corregir(UUID personaId, SolicitudEntidadVista cambios);

  /** La solicitud ya no espera: el equipo la aprobó (Donaciones responde 409). */
  final class YaAprobada extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;

    public YaAprobada() {
      super("La solicitud ya no está pendiente");
    }
  }

  /**
   * Donaciones no aceptó los datos (400): {@code campos} son los del formulario que hay que revisar
   * ({@code entidad}, {@code calle}…), en el orden en que llegaron.
   */
  final class DatosRechazados extends RuntimeException {

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
}
