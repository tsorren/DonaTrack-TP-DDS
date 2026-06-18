package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.donantes.ArchivoInputDTO;
import grupo5.donaciones.dto.donantes.ArchivoOutputDTO;
import grupo5.donaciones.models.entities.donantes.Archivo;
import grupo5.donaciones.services.ArchivoDonantesService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/donantes/archivos")
public class ArchivoDonantesController {

  private final ArchivoDonantesService importacionService;

  public ArchivoDonantesController(ArchivoDonantesService importacionService) {
    this.importacionService = importacionService;
  }

  @PostMapping
  public ResponseEntity<ArchivoOutputDTO> cargarArchivoDonantes(
      @RequestBody ArchivoInputDTO input) {
    if (input.path() == null || input.path().isBlank()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    // Registramos usando el string limpio extraído del record input
    Archivo archivo = importacionService.registrarArchivoInicial(input.path());

    importacionService.procesarArchivoMasivo(archivo);

    ArchivoOutputDTO responseBody =
        new ArchivoOutputDTO(archivo.getId(), archivo.getPath(), archivo.getEstado().toString());

    return ResponseEntity.status(HttpStatus.ACCEPTED).body(responseBody);
  }
}
