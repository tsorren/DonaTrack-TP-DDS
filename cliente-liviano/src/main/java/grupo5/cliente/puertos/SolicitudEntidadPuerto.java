package grupo5.cliente.puertos;

import grupo5.cliente.vistas.SolicitudEntidadVista;
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
}
