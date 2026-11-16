package grupo5.donaciones.dto.publico;

import java.util.UUID;

/**
 * Una entidad activa vista por un donante (R11-12, {@code GET /api/donantes/{id}/entidades}): lo
 * mismo que {@link EntidadPublicaDTO} y si ese donante ya le donó (alguna donación suya quedó
 * asignada a la entidad, en cualquier estado).
 */
public record EntidadParaDonanteDTO(UUID id, String razonSocial, String rubro, boolean yaLeDono) {}
