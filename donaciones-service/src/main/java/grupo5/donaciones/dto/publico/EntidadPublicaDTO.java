package grupo5.donaciones.dto.publico;

import java.util.UUID;

/**
 * Entidad beneficiaria para las rutas públicas (D26): solo lo que se muestra sin login. Sin
 * documento, dirección, medios de contacto ni representantes.
 */
public record EntidadPublicaDTO(UUID id, String razonSocial, String rubro) {}
