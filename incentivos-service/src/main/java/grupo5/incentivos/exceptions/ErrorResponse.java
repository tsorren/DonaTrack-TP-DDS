package grupo5.incentivos.exceptions;

import java.time.Instant;

public record ErrorResponse(String message, Instant timestamp) {
  public ErrorResponse(String message) {
    this(message, Instant.now());
  }
}
