package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.donantes.ArchivoInputDTO;
import grupo5.donaciones.dto.donantes.ArchivoOutputDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

public interface IArchivoDonantesController {

  ResponseEntity<ArchivoOutputDTO> cargarArchivoDonantes(@RequestBody ArchivoInputDTO input);
}
