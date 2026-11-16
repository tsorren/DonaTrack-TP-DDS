package grupo5.donaciones.controllers.impl;

import static grupo5.donaciones.seguridad.Acceso.ADMIN;
import static grupo5.donaciones.seguridad.Acceso.DONANTE;
import static grupo5.donaciones.seguridad.Acceso.PUBLICO;
import static grupo5.donaciones.seguridad.Acceso.SU_DONACION;

import grupo5.donaciones.controllers.IDonacionesController;
import grupo5.donaciones.dto.donaciones.FiltroDonaciones;
import grupo5.donaciones.dto.donaciones.inputs.DonacionInputDTO;
import grupo5.donaciones.dto.donaciones.outputs.ContadoresPublicosDTO;
import grupo5.donaciones.dto.donaciones.outputs.DonacionOutputDTO;
import grupo5.donaciones.dto.donaciones.outputs.EvolucionAdminDTO;
import grupo5.donaciones.dto.donaciones.outputs.ResumenAdminDTO;
import grupo5.donaciones.models.entities.actividad.AccionDeAdministracion;
import grupo5.donaciones.services.IActividadService;
import grupo5.donaciones.services.IDonacionesService;
import grupo5.donaciones.services.ParametrosListado;
import grupo5.donaciones.services.impl.ContadoresPublicosService;
import grupo5.donaciones.services.impl.EvolucionAdminService;
import grupo5.donaciones.services.impl.ResumenAdminService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize(ADMIN)
@RequestMapping("/api/donaciones")
public class DonacionesController implements IDonacionesController {

  private final IDonacionesService service;
  private final ContadoresPublicosService contadoresPublicosService;
  private final ResumenAdminService resumenAdminService;
  private final EvolucionAdminService evolucionAdminService;
  private final IActividadService actividad;

  public DonacionesController(
      IDonacionesService service,
      ContadoresPublicosService contadoresPublicosService,
      ResumenAdminService resumenAdminService,
      EvolucionAdminService evolucionAdminService,
      IActividadService actividad) {
    this.service = service;
    this.contadoresPublicosService = contadoresPublicosService;
    this.resumenAdminService = resumenAdminService;
    this.evolucionAdminService = evolucionAdminService;
    this.actividad = actividad;
  }

  @Override
  @PreAuthorize(
      DONANTE
          + "hasAuthority('REGISTRAR_DONACIONES') and principal.puedeOperarSobre(#dto.idDonante())")
  @PostMapping
  public ResponseEntity<DonacionOutputDTO> cargarDonacion(
      @Valid @RequestBody DonacionInputDTO dto) {
    DonacionOutputDTO registrada = service.cargarDonacion(dto);
    // R11-26: solo la que registra la administración; la del donante sobre sí mismo no entra.
    actividad.registrar(
        AccionDeAdministracion.DONACION_REGISTRADA, String.valueOf(registrada.id()), null);
    return ResponseEntity.status(HttpStatus.CREATED).body(registrada);
  }

  @Override
  @GetMapping
  public ResponseEntity<List<DonacionOutputDTO>> listarDonaciones(
      FiltroDonaciones filtro, ParametrosListado listado) {
    return Paginas.conTotal(service.buscar(filtro, listado));
  }

  /** Las cifras del inicio del admin (R11-01). Solo ADMIN, como el resto de la clase. */
  @Override
  @GetMapping("/resumen")
  public ResponseEntity<ResumenAdminDTO> obtenerResumen() {
    return ResponseEntity.ok(resumenAdminService.calcular());
  }

  /**
   * Los gráficos del inicio del admin (R11-42): los últimos 12 meses por mes y por categoría,
   * aparte del resumen para que una falla de las series no tape las cifras. Solo ADMIN.
   */
  @Override
  @GetMapping("/resumen/evolucion")
  public ResponseEntity<EvolucionAdminDTO> obtenerEvolucion() {
    return ResponseEntity.ok(evolucionAdminService.calcular());
  }

  /** Público (RF-01, HB-02): sin token cuando el gateway aplique la allowlist. */
  @Override
  @PreAuthorize(PUBLICO)
  @GetMapping("/contadores")
  public ResponseEntity<ContadoresPublicosDTO> obtenerContadoresPublicos() {
    return ResponseEntity.ok(contadoresPublicosService.calcular());
  }

  @Override
  @PreAuthorize(DONANTE + "hasAuthority('VER_DONACIONES') and " + SU_DONACION)
  @GetMapping("/{id}")
  public ResponseEntity<DonacionOutputDTO> obtenerDonacion(@PathVariable("id") UUID id) {
    return ResponseEntity.ok(service.obtenerDonacion(id));
  }
}
