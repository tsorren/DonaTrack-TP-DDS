package grupo5.notificaciones.exceptions;

import grupo5.common.exceptions.DonaTrackException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(DonaTrackException.class)
  public ResponseEntity<ErrorResponse> handleDonaTrackException(DonaTrackException ex) {

    return ResponseEntity.badRequest().body(new ErrorResponse(ex.getErrorCode()));
  }
}
