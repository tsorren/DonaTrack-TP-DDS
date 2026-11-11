package grupo5.notificaciones.dto.contacto;

import java.time.LocalDateTime;

/** Un mensaje de contacto para la Administración (sin la IP de origen). */
public record MensajeContactoDTO(
    long numero,
    String tema,
    String texto,
    String codigoError,
    String correo,
    String estado,
    LocalDateTime recibidoEn,
    String respuesta,
    LocalDateTime respondidoEn) {}
