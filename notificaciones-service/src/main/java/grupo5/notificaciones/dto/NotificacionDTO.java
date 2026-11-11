package grupo5.notificaciones.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificacionDTO(
    UUID id,
    String mensaje,
    String estado,
    LocalDateTime fechaCreacion,
    boolean leida,
    LocalDateTime fechaLectura,
    String tipoEvento,
    UUID referenciaId) {

  // Constructor retrocompatible para no romper tests ni llamadas previas
  public NotificacionDTO(UUID id, String mensaje, String estado, LocalDateTime fechaCreacion) {
    this(id, mensaje, estado, fechaCreacion, false, null, null, null);
  }

  // Constructor sin tipo ni referencia: las notificaciones anteriores a HB-09
  public NotificacionDTO(
      UUID id,
      String mensaje,
      String estado,
      LocalDateTime fechaCreacion,
      boolean leida,
      LocalDateTime fechaLectura) {
    this(id, mensaje, estado, fechaCreacion, leida, fechaLectura, null, null);
  }
}
