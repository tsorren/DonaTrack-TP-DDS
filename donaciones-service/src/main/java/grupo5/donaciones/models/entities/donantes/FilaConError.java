package grupo5.donaciones.models.entities.donantes;

import java.util.UUID;

/**
 * Resultado de una fila del padrón que no se pudo importar (HB-14). Sin datos de la persona: solo
 * la línea, el motivo y, si lo hay, el código del catálogo de errores.
 *
 * @param archivoId el padrón al que pertenece
 * @param linea número de línea del CSV, contando el encabezado como línea 1
 * @param motivo por qué no se importó
 * @param codigo código de {@code ErrorCatalog} cuando la causa lo trae; si no, {@code null}
 */
public record FilaConError(UUID archivoId, int linea, MotivoErrorFila motivo, String codigo) {}
