package grupo5.incentivos.controllers;

import grupo5.incentivos.services.IncentivosService.DonanteIncentivosNotFoundException;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(DonanteIncentivosNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleNotFound(DonanteIncentivosNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            Map.of(
                "timestamp",
                LocalDateTime.now().toString(),
                "status",
                404,
                "error",
                "Not Found",
                "message",
                e.getMessage()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            Map.of(
                "timestamp",
                LocalDateTime.now().toString(),
                "status",
                400,
                "error",
                "Bad Request",
                "message",
                e.getMessage()));
  }
}
