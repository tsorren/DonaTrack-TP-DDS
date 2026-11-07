package grupo5.donaciones.dto.fotos;

import java.time.Instant;

/**
 * URL prefirmada para subir la foto directo al almacenamiento, y la referencia que después se manda
 * en {@code fotoUrl} al cargar la donación.
 *
 * @param uploadUrl URL a la que se hace la subida (sin credenciales)
 * @param metodo método HTTP de la subida (siempre {@code PUT})
 * @param tipoContenido valor del header {@code Content-Type} que hay que mandar en la subida
 * @param referencia valor para {@code fotoUrl} en el ítem de la donación
 * @param expiraEn hasta cuándo sirve {@code uploadUrl}
 */
public record SubidaFotoOutputDTO(
    String uploadUrl, String metodo, String tipoContenido, String referencia, Instant expiraEn) {}
