package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.NecesidadDTO;
import grupo5.donaciones.dto.PeriodoNecesidadDTO;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface INecesidadesController {

  ResponseEntity<NecesidadDTO> crearNecesidad(NecesidadDTO dto);

  ResponseEntity<List<NecesidadDTO>> listarNecesidades();

  ResponseEntity<NecesidadDTO> obtenerNecesidadPorId(UUID id);

  ResponseEntity<Void> darDeBajaNecesidad(UUID id);

  ResponseEntity<NecesidadDTO> actualizarNecesidad(
      @PathVariable UUID id, @RequestBody NecesidadDTO dto);

  ResponseEntity<List<NecesidadDTO>> listarInsatisfechas();

  ResponseEntity<List<NecesidadDTO>> listarPorEntidad(UUID entidadId);

  ResponseEntity<PeriodoNecesidadDTO> obtenerPeriodoVigente(UUID id);
}
