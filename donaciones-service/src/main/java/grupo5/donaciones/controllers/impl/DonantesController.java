package grupo5.donaciones.controllers.impl;

import grupo5.donaciones.controllers.IDonantesController;
import grupo5.donaciones.dto.donantes.ArchivoInputDTO;
import grupo5.donaciones.dto.donantes.ArchivoOutputDTO;
import grupo5.donaciones.dto.donantes.DonanteInputDTO;
import grupo5.donaciones.dto.donantes.DonanteOutputDTO;
import grupo5.donaciones.services.IDonantesService;
import grupo5.donaciones.services.impl.ArchivoDonantesService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/donantes")
public class DonantesController implements IDonantesController {

  private final IDonantesService donantesService;
  private final ArchivoDonantesService archivoDonantesService;

  public DonantesController(
      IDonantesService donantesService, ArchivoDonantesService archivoDonantesService) {
    this.donantesService = donantesService;
    this.archivoDonantesService = archivoDonantesService;
  }

  @Override
  @PostMapping
  public ResponseEntity<DonanteOutputDTO> crearDonante(@RequestBody DonanteInputDTO dto) {
    DonanteOutputDTO nuevoDonante = donantesService.crearDonante(dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(nuevoDonante);
  }

  @Override
  @GetMapping
  public ResponseEntity<List<DonanteOutputDTO>> listarDonantes(
      @RequestParam(value = "canal", required = false) String canal) {
    List<DonanteOutputDTO> donantes = donantesService.listarDonantesPorContacto(canal);
    return ResponseEntity.ok(donantes);
  }

  @Override
  @GetMapping("/{id}")
  public ResponseEntity<DonanteOutputDTO> obtenerDonante(@PathVariable("id") UUID id) {
    DonanteOutputDTO donante = donantesService.obtenerPorId(id);
    return ResponseEntity.ok(donante);
  }

  @Override
  @PutMapping("/{id}")
  public ResponseEntity<DonanteOutputDTO> actualizarCanal(
      @PathVariable("id") UUID id, @RequestBody DonanteInputDTO dto) {
    DonanteOutputDTO donanteActualizado = donantesService.actualizarCanal(id, dto);
    return ResponseEntity.ok(donanteActualizado);
  }

  @Override
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminarDonante(@PathVariable("id") UUID id) {
    donantesService.eliminarDonante(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/archivos")
  public ResponseEntity<ArchivoOutputDTO> cargarArchivoDonantes(
      @RequestBody ArchivoInputDTO input) {
    if (input.path() == null || input.path().isBlank()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
    // TODO: Pasar logica a service

    ArchivoOutputDTO responseBody = archivoDonantesService.cargarArchivoDonantes(input);
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(responseBody);
  }
}
