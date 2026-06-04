package grupo5.donaciones.exceptions;

import grupo5.notificaciones.exceptions.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(
            AppException ex) {

        return ResponseEntity.badRequest()
                .body(new ErrorResponse(ex.getMessage()));
    }
}