package grupo5.donaciones.controllers.impl;

import static grupo5.donaciones.seguridad.Acceso.ADMIN;
import static grupo5.donaciones.seguridad.Acceso.ADMIN_O_SERVICIO;

import grupo5.donaciones.controllers.IPropuestaDeAsignacionController;
import grupo5.donaciones.dto.propuestas.ActualizarEstadoRequestDTO;
import grupo5.donaciones.dto.propuestas.DonacionEnPropuestasDTO;
import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
import grupo5.donaciones.dto.propuestas.FiltroPropuestas;
import grupo5.donaciones.dto.propuestas.PropuestaDTO;
import grupo5.donaciones.dto.propuestas.ResumenPropuestasDTO;
import grupo5.donaciones.models.entities.actividad.AccionDeAdministracion;
import grupo5.donaciones.services.EjecucionRealizada;
import grupo5.donaciones.services.IActividadService;
import grupo5.donaciones.services.IPropuestaDeAsignacionService;
import grupo5.donaciones.services.ParametrosListado;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize(ADMIN)
@RequestMapping("/api/asignaciones")
@RequiredArgsConstructor
public class PropuestaDeAsignacionController implements IPropuestaDeAsignacionController {

  private static final String SISTEMA = "SISTEMA";

  private final IPropuestaDeAsignacionService service;
  private final IActividadService actividad;

  @Override
  @PreAuthorize(ADMIN_O_SERVICIO)
  @PostMapping("/ejecuciones")
  public ResponseEntity<List<PropuestaDTO>> ejecutar() {
    EjecucionRealizada corrida = service.ejecutarAsignacion();
    List<PropuestaDTO> nuevas = corrida.propuestas();
    // R11-26: con un token de ADMIN queda en el registro; el del scheduler o de servicio, no.
    actividad.registrar(
        AccionDeAdministracion.ASIGNACION_CORRIDA,
        corrida.ejecucionId().toString(),
        String.valueOf(nuevas.size()));
    return ResponseEntity.status(HttpStatus.CREATED).body(nuevas);
  }

  /** De la más reciente a la más vieja, de a una página; el total en X-Total-Count (R11-24). */
  @Override
  @GetMapping("/ejecuciones")
  public ResponseEntity<List<EjecucionAsignacionDTO>> historial(ParametrosListado listado) {
    return Paginas.conTotal(service.historialEjecuciones(listado));
  }

  /**
   * De a una página, con los filtros y el orden resueltos en la base; el total en X-Total-Count
   * (R11-24). Sin parámetros, la página 0 de 20.
   */
  @Override
  @GetMapping("/propuestas")
  public ResponseEntity<List<PropuestaDTO>> listar(
      FiltroPropuestas filtro, ParametrosListado listado) {
    return Paginas.conTotal(service.listarPropuestas(filtro, listado));
  }

  /** Cuántas pendientes hay en cada situación (R11-24). */
  @Override
  @GetMapping("/propuestas/resumen")
  public ResponseEntity<ResumenPropuestasDTO> resumen() {
    return ResponseEntity.ok(service.resumen());
  }

  /**
   * Las donaciones que están en alguna pendiente, de a una página; el total en X-Total-Count
   * (R11-24).
   */
  @Override
  @GetMapping("/donaciones")
  public ResponseEntity<List<DonacionEnPropuestasDTO>> donaciones(
      @RequestParam(required = false) UUID categoriaId,
      @RequestParam(required = false, defaultValue = "false") boolean soloEnConflicto,
      ParametrosListado listado) {
    return Paginas.conTotal(service.donacionesEnPendientes(categoriaId, soloEnConflicto, listado));
  }

  @Override
  @PutMapping("/propuestas/{id}/estado")
  public ResponseEntity<Void> actualizarEstado(
      @PathVariable UUID id,
      @Valid @RequestBody ActualizarEstadoRequestDTO request,
      Authentication autenticacion) {
    service.actualizarEstado(id, request.estado(), autorDe(autenticacion), request.justificacion());
    actividad.propuestaDecidida(id, request.estado());
    return ResponseEntity.ok().build();
  }

  /**
   * El autor sale del token de quien llama (su {@code sub}), nunca de un dato del cliente. Sin
   * token (seguridad deshabilitada), "SISTEMA".
   */
  private static String autorDe(Authentication autenticacion) {
    if (autenticacion != null
        && autenticacion.getCredentials() instanceof Jwt token
        && token.getSubject() != null) {
      return token.getSubject();
    }
    return SISTEMA;
  }
}
