package grupo5.donaciones.models.entities.donacionesIndependientes;

import grupo5.donaciones.models.entities.necesidades.DestinoAsignacion;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Datos de la raíz con los que se reconstituye una {@link DonacionIndependiente} leída de la base
 * (mismo criterio que {@code DatosDonacion}). {@code destino} es {@code null} si no está asignada.
 */
public record DatosDonacionIndependiente(
    UUID id,
    UUID donacionOriginalId,
    EstadoDonacionIndependiente estadoActual,
    LocalDateTime fechaRegistro,
    DestinoAsignacion destino) {}
