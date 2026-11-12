package grupo5.donaciones.controllers.impl;

import static grupo5.donaciones.seguridad.Acceso.ADMIN;
import static grupo5.donaciones.seguridad.Acceso.ADMIN_O_SERVICIO;

import grupo5.donaciones.controllers.IPropuestaDeAsignacionController;
import grupo5.donaciones.dto.propuestas.ActualizarEstadoRequestDTO;
import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
import grupo5.donaciones.dto.propuestas.PropuestaDTO;
import grupo5.donaciones.services.IPropuestaDeAsignacionService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize(ADMIN)
@RequestMapping("/api/asignaciones")
@RequiredArgsConstructor
public class PropuestaDeAsignacionController implements IPropuestaDeAsignacionController {

  private static final String SISTEMA = "SISTEMA";

  private final IPropuestaDeAsignacionService service;

  @Override
  @PreAuthorize(ADMIN_O_SERVICIO)
  @PostMapping("/ejecuciones")
  public ResponseEntity<List<PropuestaDTO>> ejecutar() {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.ejecutarAsignacion());
  }

  @Override
  @GetMapping("/ejecuciones")
  public ResponseEntity<List<EjecucionAsignacionDTO>> historial() {
    return ResponseEntity.ok(service.historialEjecuciones());
  }

  @Override
  @GetMapping("/propuestas")
  public ResponseEntity<List<PropuestaDTO>> listar() {
    return ResponseEntity.ok(service.listarPropuestas());
  }

  @Override
  @PutMapping("/propuestas/{id}/estado")
  public ResponseEntity<Void> actualizarEstado(
      @PathVariable UUID id,
      @Valid @RequestBody ActualizarEstadoRequestDTO request,
      Authentication autenticacion) {
    service.actualizarEstado(id, request.estado(), autorDe(autenticacion));
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
