package grupo5.donaciones.controllers.impl;

import grupo5.donaciones.controllers.IDonantesController;
import grupo5.donaciones.dto.donantes.ArchivoInputDTO;
import grupo5.donaciones.dto.donantes.ArchivoOutputDTO;
import grupo5.donaciones.dto.donantes.DonanteInputDTO;
import grupo5.donaciones.dto.donantes.DonanteOutputDTO;
import grupo5.donaciones.services.IArchivoDonantesService;
import grupo5.donaciones.services.IDonantesService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/donantes")
public class DonantesController implements IDonantesController {

  private final IDonantesService donantesService;
  private final IArchivoDonantesService archivoDonantesService;

  public DonantesController(
      IDonantesService donantesService, IArchivoDonantesService archivoDonantesService) {
    this.donantesService = donantesService;
    this.archivoDonantesService = archivoDonantesService;
  }

  @Override
  @PostMapping
  public ResponseEntity<DonanteOutputDTO> crearDonante(@Valid @RequestBody DonanteInputDTO dto) {
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
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminarDonante(@PathVariable("id") UUID id) {
    donantesService.eliminarDonante(id);
    return ResponseEntity.noContent().build();
  }

  @Override
  @PostMapping("/archivos")
  public ResponseEntity<ArchivoOutputDTO> cargarArchivoDonantes(
      @Valid @RequestBody ArchivoInputDTO input) {
    ArchivoOutputDTO responseBody = archivoDonantesService.cargarArchivoDonantes(input);
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(responseBody);
  }
}
