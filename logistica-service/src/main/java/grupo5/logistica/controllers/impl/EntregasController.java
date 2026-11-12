package grupo5.logistica.controllers.impl;

import static grupo5.logistica.seguridad.Acceso.ADMIN;
import static grupo5.logistica.seguridad.Acceso.ADMIN_O_SERVICIO;
import static grupo5.logistica.seguridad.Acceso.CONFIRMAR_SU_ENTREGA;
import static grupo5.logistica.seguridad.Acceso.PUBLICO;
import static grupo5.logistica.seguridad.Acceso.VER_EL_SEGUIMIENTO_DE_SU_BENEFICIARIA;
import static grupo5.logistica.seguridad.Acceso.VER_EL_SEGUIMIENTO_DE_SU_DONACION;
import static grupo5.logistica.seguridad.Acceso.VER_EL_SEGUIMIENTO_DE_SU_DONANTE;
import static grupo5.logistica.seguridad.Acceso.VER_SU_ENTREGA;

import grupo5.logistica.controllers.IEntregasController;
import grupo5.logistica.dto.entregas.AdjuntarFotoRecepcionRequestDTO;
import grupo5.logistica.dto.entregas.CambioEstadoEntregaRequestDTO;
import grupo5.logistica.dto.entregas.CambioEstadoEntregaResponseDTO;
import grupo5.logistica.dto.entregas.ConsultaSeguimiento;
import grupo5.logistica.dto.entregas.CrearEntregaRequestDTO;
import grupo5.logistica.dto.entregas.EntregaMapaPublicoResponseDTO;
import grupo5.logistica.dto.entregas.EntregaResponseDTO;
import grupo5.logistica.dto.entregas.EntregasDelMapaPublico;
import grupo5.logistica.dto.entregas.SeguimientoEntregaResponseDTO;
import grupo5.logistica.dto.entregas.SeguimientosDeEntregas;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.services.IEntregasService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize(ADMIN)
@RequestMapping("/api/entregas")
public class EntregasController implements IEntregasController {
  static final String HEADER_TOTAL = "X-Total-Count";
  private final IEntregasService entregasService;

  public EntregasController(IEntregasService entregasService) {
    this.entregasService = entregasService;
  }

  @Override
  @PreAuthorize(ADMIN_O_SERVICIO)
  @PostMapping
  public ResponseEntity<EntregaResponseDTO> crear(@Valid @RequestBody CrearEntregaRequestDTO dto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(entregasService.crear(dto));
  }

  @Override
  @GetMapping
  public ResponseEntity<List<EntregaResponseDTO>> listar() {
    return ResponseEntity.ok(entregasService.listar());
  }

  @Override
  @PreAuthorize(PUBLICO)
  @GetMapping("/mapa")
  public ResponseEntity<List<EntregaMapaPublicoResponseDTO>> listarMapaPublico(
      @RequestParam(name = "desde", required = false) LocalDate desde,
      @RequestParam(name = "hasta", required = false) LocalDate hasta,
      @RequestParam(name = "limite", required = false) Integer limite) {
    EntregasDelMapaPublico mapa = entregasService.listarMapaPublico(desde, hasta, limite);
    return ResponseEntity.ok()
        .header(HEADER_TOTAL, String.valueOf(mapa.total()))
        .body(mapa.entregas());
  }

  @Override
  @PreAuthorize(VER_SU_ENTREGA)
  @GetMapping("/{id}")
  public ResponseEntity<EntregaResponseDTO> obtenerPorId(@PathVariable("id") UUID id) {
    return ResponseEntity.ok(entregasService.obtenerPorId(id));
  }

  @Override
  @PreAuthorize(VER_EL_SEGUIMIENTO_DE_SU_DONACION)
  @GetMapping("/seguimiento/donaciones/{idDonacion}")
  public ResponseEntity<SeguimientoEntregaResponseDTO> obtenerSeguimientoPorDonacion(
      @PathVariable("idDonacion") UUID idDonacion) {
    return ResponseEntity.ok(entregasService.obtenerSeguimientoPorDonacion(idDonacion));
  }

  /**
   * R11-43: de a una página ({@code page} desde 0, {@code size} de 1 a 100, 20 por defecto), con
   * {@code estado} e {@code idDonacion} repetibles; el total va en {@code X-Total-Count}.
   */
  @Override
  @PreAuthorize(VER_EL_SEGUIMIENTO_DE_SU_BENEFICIARIA)
  @GetMapping("/seguimiento/beneficiarios/{idBeneficiaria}")
  public ResponseEntity<List<SeguimientoEntregaResponseDTO>> listarSeguimientoPorBeneficiaria(
      @PathVariable("idBeneficiaria") UUID idBeneficiaria,
      @RequestParam(name = "estado", required = false) List<EstadoEntrega> estado,
      @RequestParam(name = "idDonacion", required = false) List<UUID> idDonacion,
      @RequestParam(name = "page", required = false) Integer page,
      @RequestParam(name = "size", required = false) Integer size) {
    return conTotal(
        entregasService.listarSeguimientoPorBeneficiaria(
            idBeneficiaria, new ConsultaSeguimiento(estado, idDonacion, page, size)));
  }

  /** Como el de la organización; sin {@code estado}, las activas del donante. */
  @Override
  @PreAuthorize(VER_EL_SEGUIMIENTO_DE_SU_DONANTE)
  @GetMapping("/seguimiento/donantes/{donanteId}")
  public ResponseEntity<List<SeguimientoEntregaResponseDTO>> listarSeguimientoPorDonante(
      @PathVariable("donanteId") UUID donanteId,
      @RequestParam(name = "estado", required = false) List<EstadoEntrega> estado,
      @RequestParam(name = "idDonacion", required = false) List<UUID> idDonacion,
      @RequestParam(name = "page", required = false) Integer page,
      @RequestParam(name = "size", required = false) Integer size) {
    return conTotal(
        entregasService.listarSeguimientoPorDonante(
            donanteId, new ConsultaSeguimiento(estado, idDonacion, page, size)));
  }

  private static ResponseEntity<List<SeguimientoEntregaResponseDTO>> conTotal(
      SeguimientosDeEntregas pagina) {
    return ResponseEntity.ok()
        .header(HEADER_TOTAL, String.valueOf(pagina.total()))
        .body(pagina.seguimientos());
  }

  @Override
  @PreAuthorize(CONFIRMAR_SU_ENTREGA)
  @PatchMapping("/{id}/estado")
  public ResponseEntity<EntregaResponseDTO> cambiarEstado(
      @PathVariable("id") UUID id, @Valid @RequestBody CambioEstadoEntregaRequestDTO request) {
    return ResponseEntity.ok(entregasService.cambiarEstado(id, request));
  }

  @Override
  @PreAuthorize(CONFIRMAR_SU_ENTREGA)
  @PatchMapping("/{id}/fotos")
  public ResponseEntity<EntregaResponseDTO> adjuntarFotoRecepcion(
      @PathVariable("id") UUID id, @Valid @RequestBody AdjuntarFotoRecepcionRequestDTO dto) {
    return ResponseEntity.ok(entregasService.adjuntarFotoRecepcion(id, dto));
  }

  @Override
  @PreAuthorize(VER_SU_ENTREGA)
  @GetMapping("/{id}/historial")
  public ResponseEntity<List<CambioEstadoEntregaResponseDTO>> obtenerHistorial(
      @PathVariable("id") UUID id) {
    return ResponseEntity.ok(entregasService.obtenerHistorial(id));
  }
}
