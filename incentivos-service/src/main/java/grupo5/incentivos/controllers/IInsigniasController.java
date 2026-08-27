package grupo5.incentivos.controllers;

import grupo5.incentivos.dto.InsigniaDTO;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public interface IInsigniasController {

  ResponseEntity<List<InsigniaDTO>> obtenerInsignias(@PathVariable UUID donanteId);

  ResponseEntity<Void> configurarVisibilidad(
      @PathVariable UUID donanteId,
      @PathVariable String nombreInsignia,
      @RequestParam boolean visible);
}
