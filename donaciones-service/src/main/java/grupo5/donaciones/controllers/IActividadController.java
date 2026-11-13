package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.actividad.ActividadDTO;
import grupo5.donaciones.dto.actividad.AnotacionActividadDTO;
import grupo5.donaciones.dto.actividad.FiltroActividad;
import grupo5.donaciones.services.ParametrosListado;
import java.util.List;
import org.springframework.http.ResponseEntity;

public interface IActividadController {

  ResponseEntity<List<ActividadDTO>> listar(FiltroActividad filtro, ParametrosListado listado);

  ResponseEntity<ActividadDTO> anotar(AnotacionActividadDTO anotacion);
}
