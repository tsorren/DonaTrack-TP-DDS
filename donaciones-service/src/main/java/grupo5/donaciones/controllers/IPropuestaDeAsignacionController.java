package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.propuestas.ActualizarEstadoRequestDTO;
import grupo5.donaciones.dto.propuestas.DonacionEnPropuestasDTO;
import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
import grupo5.donaciones.dto.propuestas.FiltroPropuestas;
import grupo5.donaciones.dto.propuestas.PropuestaDTO;
import grupo5.donaciones.dto.propuestas.ResumenPropuestasDTO;
import grupo5.donaciones.services.ParametrosListado;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

public interface IPropuestaDeAsignacionController {

  ResponseEntity<List<PropuestaDTO>> ejecutar();

  ResponseEntity<List<EjecucionAsignacionDTO>> historial(ParametrosListado listado);

  ResponseEntity<List<PropuestaDTO>> listar(FiltroPropuestas filtro, ParametrosListado listado);

  ResponseEntity<ResumenPropuestasDTO> resumen();

  ResponseEntity<List<DonacionEnPropuestasDTO>> donaciones(
      UUID categoriaId, boolean soloEnConflicto, ParametrosListado listado);

  ResponseEntity<Void> actualizarEstado(
      UUID id, ActualizarEstadoRequestDTO request, Authentication autenticacion);
}
