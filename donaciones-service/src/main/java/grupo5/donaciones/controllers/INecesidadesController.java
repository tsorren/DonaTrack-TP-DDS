package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.NecesidadDTO;
import grupo5.donaciones.dto.PeriodoNecesidadDTO;
import grupo5.donaciones.dto.publico.NecesidadPublicaDTO;
import grupo5.donaciones.models.entities.necesidades.EstadoNecesidad;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

public interface INecesidadesController {

  ResponseEntity<NecesidadDTO> crearNecesidad(NecesidadDTO dto);

  ResponseEntity<List<NecesidadDTO>> listarNecesidades(
      @RequestParam(required = false) UUID entidadId,
      @RequestParam(required = false) String tipo,
      @RequestParam(required = false) List<EstadoNecesidad> estado);

  ResponseEntity<List<NecesidadPublicaDTO>> listarNecesidadesPublicas(UUID entidadId);

  ResponseEntity<NecesidadDTO> obtenerNecesidad(UUID id);

  ResponseEntity<NecesidadDTO> actualizarNecesidad(UUID id, NecesidadDTO dto);

  ResponseEntity<List<PeriodoNecesidadDTO>> listarPeriodos(UUID id);

  ResponseEntity<Void> eliminarNecesidad(UUID id);
}
