package grupo5.common.responses;

import grupo5.common.exceptions.DonaTrackException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public record ErrorResponse(String code, String type, String details, LocalDateTime timestamp) {
  public ErrorResponse(DonaTrackException ex) {
    this(
        ex.getErrorCode(),
        ex.getClass().getSimpleName(),
        ex.getMessage(),
        LocalDateTime.now(ZoneOffset.UTC));
  }

  public ErrorResponse(DonaTrackException ex, String details) {
    this(
        ex.getErrorCode(),
        ex.getClass().getSimpleName(),
        details,
        LocalDateTime.now(ZoneOffset.UTC));
  }

  public ErrorResponse(Exception ex, String code, String details) {
    this(code, ex.getClass().getSimpleName(), details, LocalDateTime.now(ZoneOffset.UTC));
  }
}
