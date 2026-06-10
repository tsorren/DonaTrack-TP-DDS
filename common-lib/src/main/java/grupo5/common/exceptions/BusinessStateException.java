package grupo5.common.exceptions;

public class BusinessStateException extends DonaTrackException {
  public BusinessStateException(ErrorCatalog error) {
    super(error);
  }
}
