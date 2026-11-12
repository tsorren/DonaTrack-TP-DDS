package grupo5.donaciones.controllers.impl;

import static grupo5.donaciones.seguridad.Acceso.ADMIN;
import static grupo5.donaciones.seguridad.Acceso.DONANTE;
import static grupo5.donaciones.seguridad.Acceso.SU_DONACION_INDEPENDIENTE;

import grupo5.donaciones.controllers.IDonacionesIndependientesController;
import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.dto.donacionesIndependientes.FiltroDonacionesIndependientes;
import grupo5.donaciones.models.entities.actividad.AccionDeAdministracion;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.services.IActividadService;
import grupo5.donaciones.services.IDonacionesIndependientesService;
import grupo5.donaciones.services.ParametrosListado;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize(ADMIN)
@RequestMapping("/donaciones-independientes")
public class DonacionesIndependientesController implements IDonacionesIndependientesController {

  private final IDonacionesIndependientesService service;
  private final IActividadService actividad;

  public DonacionesIndependientesController(
      IDonacionesIndependientesService service, IActividadService actividad) {
    this.service = service;
    this.actividad = actividad;
  }

  @Override
  @PreAuthorize(
      ADMIN
          + " or hasAuthority('VER_DONACIONES') and ("
          + "hasRole('DONANTE') and principal.puedeOperarSobre(#filtro.donanteId()) or "
          + "hasRole('ENTIDAD') and principal.puedeOperarSobre(#filtro.entidadId()))")
  @GetMapping
  public ResponseEntity<List<DonacionIndependienteResponseDTO>> obtenerTodas(
      FiltroDonacionesIndependientes filtro, ParametrosListado listado) {
    return Paginas.conTotal(service.obtenerConFiltros(filtro, listado));
  }

  @Override
  @PreAuthorize(DONANTE + "hasAuthority('VER_DONACIONES') and " + SU_DONACION_INDEPENDIENTE)
  @GetMapping("/{id}")
  public ResponseEntity<DonacionIndependienteResponseDTO> obtener(@PathVariable UUID id) {
    return ResponseEntity.ok(service.obtener(id));
  }

  @Override
  @PatchMapping("/{id}/estado")
  public ResponseEntity<DonacionIndependienteResponseDTO> cambiarEstado(
      @PathVariable UUID id,
      @Valid @RequestBody CambioEstadoDonacionIndependienteRequestDTO request,
      @RequestHeader("X-Actor") String actor) {
    DonacionIndependienteResponseDTO cambiada = service.cambiarEstado(id, request, actor);
    // R11-26: el autor del registro es el sub del token, no X-Actor (RI-SVC-04).
    if (request.estado() == TipoEstadoDonacion.VENCIDA) {
      actividad.registrar(AccionDeAdministracion.DONACION_VENCIDA, id.toString(), null);
    }
    return ResponseEntity.ok(cambiada);
  }
}
