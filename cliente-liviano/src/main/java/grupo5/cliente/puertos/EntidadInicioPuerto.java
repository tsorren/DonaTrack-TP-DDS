package grupo5.cliente.puertos;

import grupo5.cliente.vistas.EntidadInicioVista;
import java.util.UUID;

/** Lo que el Inicio de la entidad suma a lo asignado: el día y sus necesidades. */
public interface EntidadInicioPuerto {

  EntidadInicioVista inicio(UUID entidad);
}
