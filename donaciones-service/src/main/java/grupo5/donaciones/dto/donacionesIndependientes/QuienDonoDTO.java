package grupo5.donaciones.dto.donacionesIndependientes;

import java.util.UUID;

/**
 * Quién donó, sin documento, dirección ni contactos (HB-20). {@code nombreVisible} es {@code null}
 * si el donante lo ocultó en su perfil o en esa donación, o si fue anonimizado. {@code donanteId}
 * va también con el nombre oculto: el cliente liviano lo usa del lado del servidor para pedir la
 * categoría a Incentivos.
 */
public record QuienDonoDTO(String tipo, String nombreVisible, UUID donanteId) {}
