package grupo5.common.seguridad;

import static grupo5.common.exceptions.ErrorCatalog.ACCESO_DENEGADO;
import static grupo5.common.exceptions.ErrorCatalog.NO_AUTENTICADO;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.responses.ErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 401 y 403 con el {@link ErrorResponse} común, antes que el manejador genérico de errores. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class RespuestasDeSeguridad {

  @ExceptionHandler(AuthenticationException.class)
  ResponseEntity<ErrorResponse> noAutenticado(AuthenticationException ex) {
    return respuesta(UNAUTHORIZED, ex, NO_AUTENTICADO, "Falta el token o no es válido");
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ErrorResponse> accesoDenegado(AccessDeniedException ex) {
    return respuesta(FORBIDDEN, ex, ACCESO_DENEGADO, "No tenés permiso para esta operación");
  }

  private static ResponseEntity<ErrorResponse> respuesta(
      HttpStatus estado, Exception ex, ErrorCatalog error, String detalle) {
    return ResponseEntity.status(estado).body(new ErrorResponse(ex, error.getCode(), detalle));
  }
}
