package grupo5.donaciones.models.entities.donaciones;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Datos de la raíz con los que se reconstituye una {@link Donacion} leída de la base (mismo
 * criterio que {@code DatosNecesidad}: evita un constructor con demasiados parámetros).
 */
public record DatosDonacion(
    UUID id,
    UUID donanteId,
    String descripcion,
    LocalDateTime fecha,
    Deposito depositoRecepcion,
    EstadoDonacion estadoActual) {}
