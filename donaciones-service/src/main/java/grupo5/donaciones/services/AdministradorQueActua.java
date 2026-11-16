package grupo5.donaciones.services;

import java.util.Optional;

/**
 * Quién de la administración hace la llamada en curso (R11-26): el {@code sub} de su token. Vacío
 * si la llamada no trae un token de ADMIN (un token de servicio, de donante o de organización, o
 * sin token): esas llamadas no dejan actividad. Nunca sale de un dato del cuerpo ni de una
 * cabecera.
 */
public interface AdministradorQueActua {

  Optional<String> cuenta();
}
