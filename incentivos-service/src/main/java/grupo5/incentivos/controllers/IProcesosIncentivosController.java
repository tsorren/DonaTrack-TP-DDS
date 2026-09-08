package grupo5.incentivos.controllers;

import org.springframework.http.ResponseEntity;

public interface IProcesosIncentivosController {

  ResponseEntity<Void> ejecutarEvaluacionInactividad();

  ResponseEntity<Void> ejecutarVerificacionRachas();
}
