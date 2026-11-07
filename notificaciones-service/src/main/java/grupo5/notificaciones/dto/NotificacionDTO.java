package grupo5.notificaciones.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificacionDTO(
    UUID id,
    String mensaje,
    String estado,
    LocalDateTime fechaCreacion,
    boolean leida,
    LocalDateTime fechaLectura) {

  // Constructor retrocompatible para no romper tests ni llamadas previas
  public NotificacionDTO(UUID id, String mensaje, String estado, LocalDateTime fechaCreacion) {
    this(id, mensaje, estado, fechaCreacion, false, null);
  }
}
