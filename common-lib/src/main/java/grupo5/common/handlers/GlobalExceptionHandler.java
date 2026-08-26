package grupo5.common.handlers;

import feign.FeignException;
import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.InfrastructureException;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.responses.ErrorResponse;
import grupo5.common.responses.FieldErrorDTO;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(RecursoNoEncontradoException.class)
  public ResponseEntity<ErrorResponse> handleRecursoNoEncontrado(RecursoNoEncontradoException ex) {
    log.warn(
        "[ERROR-HANDLER] [ERROR-CODE: {}] [EXCEPTION: {}] - Resource not found: {}",
        ex.getErrorCode(),
        ex.getClass().getSimpleName(),
        ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ErrorResponse(ex, ex.getId() != null ? ex.getId().toString() : null));
  }

  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
    log.warn(
        "[ERROR-HANDLER] [ERROR-CODE: {}] [EXCEPTION: {}] - Domain validation exception: {}",
        ex.getErrorCode(),
        ex.getClass().getSimpleName(),
        ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex) {
    List<FieldErrorDTO> fieldErrors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(
                fe ->
                    new FieldErrorDTO(fe.getField(), fe.getDefaultMessage(), fe.getRejectedValue()))
            .toList();

    String details =
        fieldErrors.isEmpty()
            ? "Error de validación en la solicitud"
            : "Validación fallida para " + fieldErrors.size() + " campo(s)";

    log.warn(
        "[ERROR-HANDLER] [ERROR-CODE: {}] [EXCEPTION: {}] - Bean validation failed: {}",
        ErrorCatalog.ARGUMENTO_INVALIDO.getCode(),
        ex.getClass().getSimpleName(),
        details);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            new ErrorResponse(
                ErrorCatalog.ARGUMENTO_INVALIDO.getCode(),
                ex.getClass().getSimpleName(),
                details,
                fieldErrors));
  }
  // PENDIENTE 4a
  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(
      HandlerMethodValidationException ex) {
    log.warn(
        "[ERROR-HANDLER] [ERROR-CODE: {}] [EXCEPTION: {}] - Handler method validation failed: {}",
        ErrorCatalog.ARGUMENTO_INVALIDO.getCode(),
        ex.getClass().getSimpleName(),
        ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            new ErrorResponse(
                ex,
                ErrorCatalog.ARGUMENTO_INVALIDO.getCode(),
                "Parámetros de solicitud no válidos."));
  }
  // PENDIENTE 4b
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
    List<FieldErrorDTO> fieldErrors =
        ex.getConstraintViolations().stream()
            .map(
                cv ->
                    new FieldErrorDTO(
                        cv.getPropertyPath().toString(), cv.getMessage(), cv.getInvalidValue()))
            .toList();

    log.warn(
        "[ERROR-HANDLER] [ERROR-CODE: {}] [EXCEPTION: {}] - Constraint violation: {}",
        ErrorCatalog.ARGUMENTO_INVALIDO.getCode(),
        ex.getClass().getSimpleName(),
        ex.getMessage());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            new ErrorResponse(
                ErrorCatalog.ARGUMENTO_INVALIDO.getCode(),
                ex.getClass().getSimpleName(),
                "Violación de restricciones de validación",
                fieldErrors));
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<ErrorResponse> handleMissingRequestHeader(
      MissingRequestHeaderException ex) {
    String detail = "El encabezado '" + ex.getHeaderName() + "' es obligatorio.";
    log.warn(
        "[ERROR-HANDLER] [ERROR-CODE: {}] [EXCEPTION: {}] - Missing header: {}",
        ErrorCatalog.ARGUMENTO_INVALIDO.getCode(),
        ex.getClass().getSimpleName(),
        detail);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse(ex, ErrorCatalog.ARGUMENTO_INVALIDO.getCode(), detail));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingParameter(
      MissingServletRequestParameterException ex) {
    String detail = "El parámetro '" + ex.getParameterName() + "' es obligatorio.";
    log.warn(
        "[ERROR-HANDLER] [ERROR-CODE: {}] [EXCEPTION: {}] - Missing query param: {}",
        ErrorCatalog.ARGUMENTO_INVALIDO.getCode(),
        ex.getClass().getSimpleName(),
        detail);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse(ex, ErrorCatalog.ARGUMENTO_INVALIDO.getCode(), detail));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex) {
    String detail = "El cuerpo de la solicitud JSON es inválido o está malformado.";
    log.warn(
        "[ERROR-HANDLER] [ERROR-CODE: {}] [EXCEPTION: {}] - Malformed JSON body: {}",
        ErrorCatalog.ARGUMENTO_INVALIDO.getCode(),
        ex.getClass().getSimpleName(),
        ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse(ex, ErrorCatalog.ARGUMENTO_INVALIDO.getCode(), detail));
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
        ex.getMessage());
    return ResponseEntity.status(status).body(new ErrorResponse(ex));
  }

  @ExceptionHandler(FeignException.class)
  public ResponseEntity<ErrorResponse> handleFeignException(FeignException ex) {
    int statusCode = ex.status() > 0 ? ex.status() : HttpStatus.BAD_GATEWAY.value();
    HttpStatus status = HttpStatus.resolve(statusCode);
    if (status == null) {
      status = HttpStatus.BAD_GATEWAY;
    }

    log.error(
        "[ERROR-HANDLER] [ERROR-CODE: {}] [EXCEPTION: FeignException] - Remote service call failed with HTTP {}: {}",
        ErrorCatalog.ERROR_INTERNO.getCode(),
        statusCode,
        ex.getMessage());
    // PENDIENTE 3
    return ResponseEntity.status(status)
        .body(
            new ErrorResponse(
                ex,
                ErrorCatalog.ERROR_INTERNO.getCode(),
                "Error en la comunicación con el servicio remoto."));
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
        ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse(ex, ErrorCatalog.ARGUMENTO_INVALIDO.getCode(), ex.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
    log.error(
        "[ERROR-HANDLER] [ERROR-CODE: {}] [EXCEPTION: {}] - Uncaught internal server error: {}",
        ErrorCatalog.ERROR_INTERNO.getCode(),
        ex.getClass().getSimpleName(),
        ex.getMessage(),
        ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorResponse(ex, ErrorCatalog.ERROR_INTERNO.getCode(), "Internal Server Error"));
  }
}
