package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.NecesidadDTO;
import grupo5.donaciones.dto.PeriodoNecesidadDTO;
import grupo5.donaciones.services.INecesidadesService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/necesidades")
public class NecesidadesController implements INecesidadesController {

  private final INecesidadesService necesidadesService;

  // Inyección de dependencias por constructor
  public NecesidadesController(INecesidadesService necesidadesService) {
    this.necesidadesService = necesidadesService;
  }

  // ENDPOINTS: operaciones CRUD

  // POST: crear necesidad
  @Override
  @PostMapping
  public ResponseEntity<NecesidadDTO> crearNecesidad(@RequestBody NecesidadDTO dto) {
    NecesidadDTO creada = necesidadesService.guardar(dto);
    // Retorna HTTP 201 Created junto con el objeto persistido en memoria
    return ResponseEntity.status(HttpStatus.CREATED).body(creada);
  }

  // GET: mostrar todas las necesidades
  @Override
  @GetMapping
  public ResponseEntity<List<NecesidadDTO>> listarNecesidades() {
    List<NecesidadDTO> lista = necesidadesService.obtenerTodas();
    return ResponseEntity.ok(lista);
  }

  // GET: mostrar necesidad por ID
  @Override
  @GetMapping("/{id}")
  public ResponseEntity<NecesidadDTO> obtenerNecesidadPorId(@PathVariable UUID id) {
    NecesidadDTO dto = necesidadesService.obtenerPorId(id);
    return ResponseEntity.ok(dto);
  }

  // DELETE: eliminar necesidad por ID
  @Override
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> darDeBajaNecesidad(@PathVariable UUID id) {
    necesidadesService.eliminar(id);
    // Retorna HTTP 204 No Content (Éxito sin cuerpo de respuesta)
    return ResponseEntity.noContent().build();
  }

  // PUT: actualizar necesidad por ID
  @Override
  @PutMapping("/{id}")
  public ResponseEntity<NecesidadDTO> actualizarNecesidad(
      @PathVariable UUID id, @RequestBody NecesidadDTO dto) {
    NecesidadDTO actualizada = necesidadesService.actualizar(id, dto);
    return ResponseEntity.ok(actualizada);
  }

  // Endpoints no CRUD
  @Override
  @GetMapping("/insatisfechas")
  public ResponseEntity<List<NecesidadDTO>> listarInsatisfechas() {
    List<NecesidadDTO> lista = necesidadesService.obtenerNecesidadesInsatisfechas();
    return ResponseEntity.ok(lista);
  }

  // GET: /api/necesidades/entidad/{entidadId}
  @Override
  @GetMapping("/entidad/{entidadId}")
  public ResponseEntity<List<NecesidadDTO>> listarPorEntidad(@PathVariable UUID entidadId) {
    List<NecesidadDTO> lista = necesidadesService.obtenerNecesidadesPorEntidad(entidadId);
    return ResponseEntity.ok(lista);
  }

  // GET: /api/necesidades/{id}/periodo-vigente
  @Override
  @GetMapping("/{id}/periodo-vigente")
  public ResponseEntity<PeriodoNecesidadDTO> obtenerPeriodoVigente(@PathVariable UUID id) {
    PeriodoNecesidadDTO periodo = necesidadesService.obtenerPeriodoVigente(id);
    return ResponseEntity.ok(periodo);
  }
}
