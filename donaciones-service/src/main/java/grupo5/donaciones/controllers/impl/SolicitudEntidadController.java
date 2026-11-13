package grupo5.donaciones.controllers.impl;

import static grupo5.donaciones.seguridad.Acceso.ADMIN;
import static grupo5.donaciones.seguridad.Acceso.CON_SESION;
import static grupo5.donaciones.seguridad.Acceso.SU_ID;

import grupo5.donaciones.controllers.ISolicitudEntidadController;
import grupo5.donaciones.dto.personas.SolicitudEntidadDTO;
import grupo5.donaciones.services.ISolicitudEntidadService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * La solicitud de una entidad que espera la aprobación (R11-41): la lee y la corrige la propia
 * cuenta, que todavía no tiene rol, con su token. El admin pasa siempre.
 */
@RestController
@RequestMapping("/api/personas/{id}/solicitud-entidad")
@PreAuthorize(ADMIN + " or " + CON_SESION + " and " + SU_ID)
public class SolicitudEntidadController implements ISolicitudEntidadController {

  private final ISolicitudEntidadService solicitudes;

  public SolicitudEntidadController(ISolicitudEntidadService solicitudes) {
    this.solicitudes = solicitudes;
  }

  @Override
  @GetMapping
  public ResponseEntity<SolicitudEntidadDTO> consultarSolicitud(@PathVariable("id") UUID id) {
    return ResponseEntity.ok(solicitudes.consultar(id));
  }

  @Override
  @PutMapping
  public ResponseEntity<SolicitudEntidadDTO> corregirSolicitud(
      @PathVariable("id") UUID id, @Valid @RequestBody SolicitudEntidadDTO solicitud) {
    return ResponseEntity.ok(solicitudes.corregir(id, solicitud));
  }
}
