package grupo5.donaciones.dto.donantes;

/**
 * Una fila del padrón que no se importó (HB-14).
 *
 * @param linea número de línea del CSV, contando el encabezado como línea 1
 * @param motivo FORMATO_INVALIDO, DATOS_INVALIDOS, DONANTE_DADO_DE_BAJA,
 *     PERSONA_DUPLICADA_EN_CONFLICTO o ERROR_INESPERADO
 * @param codigo código del catálogo de errores, o {@code null}
 */
public record FilaConErrorDTO(int linea, String motivo, String codigo) {}
