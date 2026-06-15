package grupo5.common.exceptions;

import lombok.Getter;

@Getter
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

  public String getErrorCode() {
    return error.getCode();
  }
}
