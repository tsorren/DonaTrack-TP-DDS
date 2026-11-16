package grupo5.donaciones.dto.entidadBeneficiaria;

/**
 * De dónde sale el alta de una organización (R11-26): {@code SOLICITUD} cuando la administración
 * aprueba la solicitud que la organización mandó desde el sitio; {@code ADMIN} cuando la registra
 * ella misma. Solo cambia la acción que queda en el registro de actividad.
 */
public enum OrigenEntidad {
  SOLICITUD,
  ADMIN
}
