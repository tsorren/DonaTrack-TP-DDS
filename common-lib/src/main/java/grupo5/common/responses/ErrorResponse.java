package grupo5.common.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import grupo5.common.exceptions.DonaTrackException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.slf4j.MDC;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    String code,
    String type,
    String details,
    String traceId,
    LocalDateTime timestamp,
    List<FieldErrorDTO> errors) {

  public ErrorResponse(
      String code, String type, String details, String traceId, LocalDateTime timestamp) {
    this(code, type, details, resolveTraceId(traceId), timestamp, null);
  }

  public ErrorResponse(String code, String type, String details, LocalDateTime timestamp) {
    this(code, type, details, resolveTraceId(null), timestamp, null);
  }

  public ErrorResponse(DonaTrackException ex) {
    this(
        ex.getErrorCode(),
        ex.getClass().getSimpleName(),
        ex.getMessage(),
        resolveTraceId(null),
        LocalDateTime.now(ZoneOffset.UTC),
        null);
  }

  public ErrorResponse(DonaTrackException ex, String details) {
    this(
        ex.getErrorCode(),
        ex.getClass().getSimpleName(),
        details,
        resolveTraceId(null),
        LocalDateTime.now(ZoneOffset.UTC),
        null);
  }

  public ErrorResponse(Exception ex, String code, String details) {
    this(
        code,
        ex.getClass().getSimpleName(),
        details,
        resolveTraceId(null),
        LocalDateTime.now(ZoneOffset.UTC),
        null);
  }

  public ErrorResponse(String code, String type, String details, List<FieldErrorDTO> errors) {
    this(code, type, details, resolveTraceId(null), LocalDateTime.now(ZoneOffset.UTC), errors);
  }

  private static String resolveTraceId(String explicitTraceId) {
    if (explicitTraceId != null && !explicitTraceId.isBlank()) {
      return explicitTraceId;
    }
    String mdcTraceId = MDC.get("traceId");
    if (mdcTraceId != null && !mdcTraceId.isBlank()) {
      return mdcTraceId;
    }
    String mdcTraceIdAlt = MDC.get("trace_id");
    if (mdcTraceIdAlt != null && !mdcTraceIdAlt.isBlank()) {
      return mdcTraceIdAlt;
    }
    return UUID.randomUUID().toString().replace("-", "");
  }
}
