package grupo5.donaciones.dto.donantes;

import java.io.InputStream;

/**
 * Padrón CSV recibido por HTTP, ya sin tipos de Spring: el nombre original solo se usa para validar
 * la extensión (nunca como clave del objeto).
 *
 * @param nombreOriginal nombre del archivo que mandó el cliente (puede ser {@code null})
 * @param tamanio tamaño en bytes
 * @param contenido el contenido; lo cierra quien lo abrió
 */
public record ArchivoDonantesSubido(String nombreOriginal, long tamanio, InputStream contenido) {}
