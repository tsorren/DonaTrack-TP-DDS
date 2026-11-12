package grupo5.incentivos.controllers;

import static grupo5.incentivos.seguridad.Acceso.ADMIN;
import static grupo5.incentivos.seguridad.Acceso.ADMIN_O_SERVICIO;

import grupo5.incentivos.services.IInactividadService;
import grupo5.incentivos.services.IMisionesDonacionService;
import java.time.YearMonth;
import java.time.ZoneId;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Disparo manual/on-demand de los procesos batch que normalmente corren via
// InactividadJob y RachaJob. Pensados para testing y uso administrativo.
@RestController
@PreAuthorize(ADMIN)
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
  @PreAuthorize(ADMIN_O_SERVICIO)
  @PostMapping("/evaluaciones-inactividad")
  public ResponseEntity<Void> ejecutarEvaluacionInactividad() {
    inactividadService.procesarInactividad();
    return ResponseEntity.ok().build();
  }

  @Override
  @PreAuthorize(ADMIN_O_SERVICIO)
  @PostMapping("/verificaciones-racha")
  public ResponseEntity<Void> ejecutarVerificacionRachas() {
    misionesDonacionService.verificarRachasVencidas(YearMonth.now(ZoneId.systemDefault()));
    return ResponseEntity.ok().build();
  }
}
