package grupo5.common.responses;

public record FieldErrorDTO(String field, String message, Object rejectedValue) {
  public FieldErrorDTO(String field, String message) {
    this(field, message, null);
  }
}
