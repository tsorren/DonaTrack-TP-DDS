package grupo5.incentivos.controllers;

import grupo5.incentivos.services.IInactividadService;
import grupo5.incentivos.services.IMisionesDonacionService;
import java.time.YearMonth;
import java.time.ZoneId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Disparo manual/on-demand de los procesos batch que normalmente corren via
// InactividadJob y RachaJob. Pensados para testing y uso administrativo.
@RestController
@RequestMapping("/api/incentivos")
public class ProcesosIncentivosController implements IProcesosIncentivosController {

  private final IInactividadService inactividadService;
  private final IMisionesDonacionService misionesDonacionService;

  public ProcesosIncentivosController(
      IInactividadService inactividadService, IMisionesDonacionService misionesDonacionService) {
    this.inactividadService = inactividadService;
    this.misionesDonacionService = misionesDonacionService;
  }

  @Override
  @PostMapping("/evaluaciones-inactividad")
  public ResponseEntity<Void> ejecutarEvaluacionInactividad() {
    inactividadService.procesarInactividad();
    return ResponseEntity.ok().build();
  }

  @Override
  @PostMapping("/verificaciones-racha")
  public ResponseEntity<Void> ejecutarVerificacionRachas() {
    misionesDonacionService.verificarRachasVencidas(YearMonth.now(ZoneId.systemDefault()));
    return ResponseEntity.ok().build();
  }
}
