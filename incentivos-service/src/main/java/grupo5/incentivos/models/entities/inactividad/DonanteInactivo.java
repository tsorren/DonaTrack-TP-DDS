package grupo5.incentivos.models.entities.inactividad;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Resultado de detección de inactividad para un donante. Es un value object inmutable: lo arma cada
 * {@link CriterioInactividad}, los junta el {@link GestorDeInactivos} en una sola lista, y la usa
 * el service para comunicarse con el cliente de notificaciones.
 */
public record DonanteInactivo(
    UUID idDonante, UUID idPersona, Integer diasInactivo, LocalDate fecha) {}
