package grupo5.common.exceptions;

public abstract class DonaTrackException extends RuntimeException {
  private final ErrorCatalog error;

  protected DonaTrackException(ErrorCatalog error) {
    super();
    this.error = error;
  }

  protected DonaTrackException(ErrorCatalog error, Throwable cause) {
    super(cause);
    this.error = error;
  }

  public ErrorCatalog getError() {
    return error;
  }

  public String getErrorCode() {
    return error.getCode();
  }
}
