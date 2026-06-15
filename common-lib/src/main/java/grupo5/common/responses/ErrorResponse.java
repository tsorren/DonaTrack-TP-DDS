package grupo5.common.responses;

import grupo5.common.exceptions.DonaTrackException;
import java.time.LocalDateTime;

public record ErrorResponse(String code, String type, String details, LocalDateTime timestamp) {
  public ErrorResponse(DonaTrackException ex) {
    this(ex.getErrorCode(), ex.getClass().getSimpleName(), ex.getMessage(), LocalDateTime.now());
  }

  public ErrorResponse(DonaTrackException ex, String details) {
    this(ex.getErrorCode(), ex.getClass().getSimpleName(), details, LocalDateTime.now());
  }

  public ErrorResponse(Exception ex, String code, String details) {
    this(code, ex.getClass().getSimpleName(), details, LocalDateTime.now());
  }
}
