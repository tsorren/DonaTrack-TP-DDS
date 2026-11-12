package grupo5.donaciones.controllers.impl;

import static grupo5.donaciones.seguridad.Acceso.ADMIN;
import static grupo5.donaciones.seguridad.Acceso.ENTIDAD;
import static grupo5.donaciones.seguridad.Acceso.PUBLICO;
import static grupo5.donaciones.seguridad.Acceso.SU_NECESIDAD;

import grupo5.donaciones.controllers.INecesidadesController;
import grupo5.donaciones.dto.NecesidadDTO;
import grupo5.donaciones.dto.PeriodoNecesidadDTO;
import grupo5.donaciones.dto.publico.NecesidadPublicaDTO;
import grupo5.donaciones.models.entities.necesidades.EstadoNecesidad;
import grupo5.donaciones.services.INecesidadesService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/necesidades")
public class NecesidadesController implements INecesidadesController {

  private final INecesidadesService necesidadesService;

  public NecesidadesController(INecesidadesService necesidadesService) {
    this.necesidadesService = necesidadesService;
  }

  @Override
  @PreAuthorize(
      ENTIDAD
          + "hasAuthority('GESTIONAR_NECESIDADES') and "
          + "principal.puedeOperarSobre(#dto.idEntidad())")
  @PostMapping
  public ResponseEntity<NecesidadDTO> crearNecesidad(@Valid @RequestBody NecesidadDTO dto) {
    NecesidadDTO creada = necesidadesService.guardar(dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(creada);
  }

  @Override
  @PreAuthorize(
      ENTIDAD + "hasAuthority('VER_NECESIDADES') and principal.puedeOperarSobre(#entidadId)")
  @GetMapping
  public ResponseEntity<List<NecesidadDTO>> listarNecesidades(
      @RequestParam(required = false) UUID entidadId,
      @RequestParam(required = false) String tipo,
      @RequestParam(required = false) List<EstadoNecesidad> estado) {
    List<NecesidadDTO> lista = necesidadesService.listarConFiltros(entidadId, tipo, estado);
    return ResponseEntity.ok(lista);
  }

  @Override
  @PreAuthorize(PUBLICO)
  @GetMapping("/publicas")
  public ResponseEntity<List<NecesidadPublicaDTO>> listarNecesidadesPublicas(
      @RequestParam(required = false) UUID entidadId) {
    return ResponseEntity.ok(necesidadesService.listarPublicas(entidadId));
  }

  @Override
  @PreAuthorize(ENTIDAD + "hasAuthority('VER_NECESIDADES') and " + SU_NECESIDAD)
  @GetMapping("/{id}")
  public ResponseEntity<NecesidadDTO> obtenerNecesidad(@PathVariable UUID id) {
    return ResponseEntity.ok(necesidadesService.obtenerPorId(id));
  }

  @Override
  @PreAuthorize(ENTIDAD + "hasAuthority('GESTIONAR_NECESIDADES') and " + SU_NECESIDAD)
  @PutMapping("/{id}")
  public ResponseEntity<NecesidadDTO> actualizarNecesidad(
      @PathVariable UUID id, @Valid @RequestBody NecesidadDTO dto) {
    return ResponseEntity.ok(necesidadesService.actualizar(id, dto));
  }

  @Override
  @PreAuthorize(ENTIDAD + "hasAuthority('VER_NECESIDADES') and " + SU_NECESIDAD)
  @GetMapping("/{id}/periodos")
  public ResponseEntity<List<PeriodoNecesidadDTO>> listarPeriodos(@PathVariable UUID id) {
    return ResponseEntity.ok(necesidadesService.listarPeriodos(id));
  }

  @Override
  @PreAuthorize(ENTIDAD + "hasAuthority('GESTIONAR_NECESIDADES') and " + SU_NECESIDAD)
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminarNecesidad(@PathVariable UUID id) {
    necesidadesService.eliminar(id);
    return ResponseEntity.noContent().build();
  }
}
