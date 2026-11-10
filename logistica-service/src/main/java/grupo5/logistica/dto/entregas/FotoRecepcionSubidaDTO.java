package grupo5.logistica.dto.entregas;

import java.util.UUID;

/**
 * Una foto de recepción ya subida: su id y su URL estable, que es la que la entidad manda después
 * en {@code fotosRecepcion} al confirmar la entrega (ADR-E6-08).
 */
public record FotoRecepcionSubidaDTO(UUID fotoId, String url) {}
