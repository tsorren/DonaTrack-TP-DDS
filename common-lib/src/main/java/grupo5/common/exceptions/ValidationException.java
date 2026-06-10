package grupo5.common.exceptions;

public class ValidationException extends DonaTrackException {
  public ValidationException(ErrorCatalog error) {
    super(error);
  }
}
