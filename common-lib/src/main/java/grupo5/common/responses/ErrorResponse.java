package grupo5.common.responses;

import java.time.LocalDateTime;

public record ErrorResponse(
    boolean success, String message, String details, LocalDateTime timestamp) {
  public static ErrorResponse of(String message, String details) {
    return new ErrorResponse(false, message, details, LocalDateTime.now());
  }
}
