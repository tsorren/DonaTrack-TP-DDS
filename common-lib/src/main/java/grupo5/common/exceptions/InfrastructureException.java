package grupo5.common.exceptions;

public class InfrastructureException extends DonaTrackException {
  public InfrastructureException(ErrorCatalog error, Throwable cause) {
    super(error, cause);
  }
}
