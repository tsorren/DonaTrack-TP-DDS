package grupo5.tests.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ErrorResponseTestDTO(
    String code,
    String type,
    String details,
    String traceId,
    String timestamp,
    List<FieldErrorTestDTO> errors) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record FieldErrorTestDTO(String field, String message, Object rejectedValue) {}
}
