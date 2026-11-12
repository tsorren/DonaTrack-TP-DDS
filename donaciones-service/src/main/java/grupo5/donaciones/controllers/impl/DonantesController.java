package grupo5.donaciones.controllers.impl;

import static grupo5.donaciones.seguridad.Acceso.ADMIN;
import static grupo5.donaciones.seguridad.Acceso.ADMIN_O_SERVICIO;
import static grupo5.donaciones.seguridad.Acceso.DONANTE;
import static grupo5.donaciones.seguridad.Acceso.SU_ID;

import grupo5.donaciones.controllers.IDonantesController;
import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.donantes.ArchivoDonantesSubido;
import grupo5.donaciones.dto.donantes.ArchivoOutputDTO;
import grupo5.donaciones.dto.donantes.DonanteInputDTO;
import grupo5.donaciones.dto.donantes.DonanteOutputDTO;
import grupo5.donaciones.dto.donantes.FilaConErrorDTO;
import grupo5.donaciones.dto.donantes.FiltroArchivos;
import grupo5.donaciones.dto.donantes.FiltroDonantes;
import grupo5.donaciones.dto.donantes.PreferenciasDonanteDTO;
import grupo5.donaciones.dto.publico.EntidadParaDonanteDTO;
import grupo5.donaciones.models.entities.actividad.AccionDeAdministracion;
import grupo5.donaciones.services.IActividadService;
import grupo5.donaciones.services.IArchivoDonantesService;
import grupo5.donaciones.services.IDonantesService;
import grupo5.donaciones.services.IEntidadBeneficiariaService;
import grupo5.donaciones.services.ParametrosListado;
import grupo5.donaciones.services.ResultadoRegistro;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@PreAuthorize(ADMIN)
@RequestMapping("/api/donantes")
public class DonantesController implements IDonantesController {

  private final IDonantesService donantesService;
  private final IArchivoDonantesService archivoDonantesService;
  private final IEntidadBeneficiariaService entidadesService;
  private final IActividadService actividad;

  public DonantesController(
      IDonantesService donantesService,
      IArchivoDonantesService archivoDonantesService,
      IEntidadBeneficiariaService entidadesService,
      IActividadService actividad) {
    this.donantesService = donantesService;
    this.archivoDonantesService = archivoDonantesService;
    this.entidadesService = entidadesService;
    this.actividad = actividad;
  }

  @Override
  @PreAuthorize(ADMIN_O_SERVICIO)
  @PostMapping
  public ResponseEntity<DonanteOutputDTO> crearDonante(@Valid @RequestBody DonanteInputDTO dto) {
    ResultadoRegistro<DonanteOutputDTO> resultado = donantesService.crearDonante(dto);
    if (resultado.creado()) {
      // R11-26: con un token de ADMIN; el registro desde el sitio (token de servicio), no.
      actividad.donanteRegistrado(resultado.recurso());
    }
    HttpStatus status = resultado.creado() ? HttpStatus.CREATED : HttpStatus.OK;
    return ResponseEntity.status(status).body(resultado.recurso());
  }

  @Override
  @GetMapping
  public ResponseEntity<List<DonanteOutputDTO>> listarDonantes(
      FiltroDonantes filtro, ParametrosListado listado) {
    return Paginas.conTotal(donantesService.buscar(filtro, listado));
  }

  @Override
  @PreAuthorize(DONANTE + SU_ID)
  @GetMapping("/{id}")
  public ResponseEntity<DonanteOutputDTO> obtenerDonante(@PathVariable("id") UUID id) {
    DonanteOutputDTO donante = donantesService.obtenerPorId(id);
    return ResponseEntity.ok(donante);
  }

  /**
   * R11-12: las entidades activas vistas por el donante, con la búsqueda, la categoría y "solo a
   * las que doné" resueltas en Donaciones, de a una página y con el total en {@code X-Total-Count}.
   */
  @Override
  @PreAuthorize(DONANTE + SU_ID)
  @GetMapping("/{id}/entidades")
  public ResponseEntity<List<EntidadParaDonanteDTO>> listarEntidades(
      @PathVariable("id") UUID id,
      @RequestParam(required = false) String nombre,
      @RequestParam(required = false) UUID categoriaId,
      @RequestParam(defaultValue = "false") boolean soloDonadas,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size) {
    return Paginas.conTotal(
        entidadesService.buscarParaDonante(id, nombre, categoriaId, soloDonadas, page, size));
  }

  @Override
  @PreAuthorize(DONANTE + SU_ID)
  @PatchMapping("/{id}/preferencias")
  public ResponseEntity<DonanteOutputDTO> actualizarPreferencias(
      @PathVariable("id") UUID id, @Valid @RequestBody PreferenciasDonanteDTO dto) {
    return ResponseEntity.ok(donantesService.actualizarPreferencias(id, dto.mostrarNombre()));
  }

  @Override
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminarDonante(@PathVariable("id") UUID id) {
    donantesService.eliminarDonante(id);
    return ResponseEntity.noContent().build();
  }

  @Override
  @PostMapping(value = "/archivos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ArchivoOutputDTO> cargarArchivoDonantes(
      @RequestParam("archivo") MultipartFile archivo) throws IOException {
    try (InputStream contenido = archivo.getInputStream()) {
      ArchivoOutputDTO responseBody =
          archivoDonantesService.cargarArchivoDonantes(
              new ArchivoDonantesSubido(
                  archivo.getOriginalFilename(), archivo.getSize(), contenido));
      actividad.registrar(
          AccionDeAdministracion.IMPORTACION_SUBIDA,
          String.valueOf(responseBody.id()),
          responseBody.nombre());
      return ResponseEntity.status(HttpStatus.ACCEPTED).body(responseBody);
    }
  }

  /** R11-20: las importaciones, de a una página, con el total en {@code X-Total-Count}. */
  @Override
  @GetMapping("/archivos")
  public ResponseEntity<List<ArchivoOutputDTO>> listarArchivosDonantes(
      FiltroArchivos filtro, ParametrosListado listado) {
    return Paginas.conTotal(archivoDonantesService.listar(filtro, listado));
  }

  @Override
  @GetMapping("/archivos/{id}")
  public ResponseEntity<ArchivoOutputDTO> obtenerArchivoDonantes(@PathVariable("id") UUID id) {
    ArchivoOutputDTO responseBody = archivoDonantesService.obtenerPorId(id);
    return ResponseEntity.ok(responseBody);
  }

  @Override
  @GetMapping("/archivos/{id}/filas-con-error")
  public ResponseEntity<List<FilaConErrorDTO>> obtenerFilasConError(
      @PathVariable("id") UUID id,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size) {
    Pagina<FilaConErrorDTO> pagina = archivoDonantesService.obtenerFilasConError(id, page, size);
    return ResponseEntity.ok()
        .header(EntidadBeneficiariaController.HEADER_TOTAL, String.valueOf(pagina.total()))
        .body(pagina.elementos());
  }
}
