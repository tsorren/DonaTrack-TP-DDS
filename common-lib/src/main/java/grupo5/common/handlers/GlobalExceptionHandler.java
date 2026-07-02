package grupo5.common.handlers;

import grupo5.common.exceptions.*;
import grupo5.common.responses.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(RecursoNoEncontradoException.class)
  public ResponseEntity<ErrorResponse> handleRecursoNoEncontrado(RecursoNoEncontradoException ex) {
    log.error(
        "[ERROR-HANDLER] [ERROR-CODE: {}] [EXCEPTION: {}] - Resource not found exception: {}",
        ex.getErrorCode(),
        ex.getClass().getSimpleName(),
        ex.getMessage(),
        ex);
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ErrorResponse(ex, ex.getId() != null ? ex.getId().toString() : null));
  }

  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
    log.warn(
        "[ERROR-HANDLER] [ERROR-CODE: {}] [EXCEPTION: {}] - Validation exception: {}",
        ex.getErrorCode(),
        ex.getClass().getSimpleName(),
        ex.getMessage(),
        ex);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex));
  }

  @ExceptionHandler(BusinessStateException.class)
  public ResponseEntity<ErrorResponse> handleBusinessState(BusinessStateException ex) {
    HttpStatus status = HttpStatus.CONFLICT;
    if (ex.getError() == ErrorCatalog.DONANTE_INCENTIVOS_NO_ENCONTRADO
        || ex.getError() == ErrorCatalog.INSIGNIA_NO_ENCONTRADA) {
      status = HttpStatus.NOT_FOUND;
    }
    log.warn(
        "[ERROR-HANDLER] [ERROR-CODE: {}] [EXCEPTION: {}] - Business state exception: {}",
        ex.getErrorCode(),
        ex.getClass().getSimpleName(),
        ex.getMessage(),
        ex);
    return ResponseEntity.status(status).body(new ErrorResponse(ex));
  }

  @ExceptionHandler(InfrastructureException.class)
  public ResponseEntity<ErrorResponse> handleInfrastructure(InfrastructureException ex) {
    log.error(
        "[ERROR-HANDLER] [ERROR-CODE: {}] [EXCEPTION: {}] - Infrastructure exception: {}",
        ex.getErrorCode(),
        ex.getClass().getSimpleName(),
        ex.getMessage(),
        ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(ex));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
    log.warn(
        "[ERROR-HANDLER] [ERROR-CODE: {}] [EXCEPTION: {}] - Illegal argument exception: {}",
        ErrorCatalog.ARGUMENTO_INVALIDO.getCode(),
        ex.getClass().getSimpleName(),
        ex.getMessage(),
        ex);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse(ex, ErrorCatalog.ARGUMENTO_INVALIDO.getCode(), ex.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
    log.error(
        "[ERROR-HANDLER] [ERROR-CODE: {}] [EXCEPTION: {}] - Internal server error occurred: {}",
        ErrorCatalog.ERROR_INTERNO.getCode(),
        ex.getClass().getSimpleName(),
        ex.getMessage(),
        ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorResponse(ex, ErrorCatalog.ERROR_INTERNO.getCode(), "Internal Server Error"));
  }
}
