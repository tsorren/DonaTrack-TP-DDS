package grupo5.donaciones.services;

import grupo5.donaciones.models.repositories.IAsignacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AsignacionService {
  private final IAsignacionRepository asignacionRepository;

  //    private final GestorAlgoritmos gestorAlgoritmos;
  //
  //    public List<Propuesta> ejecutarAsignacion() {
  //
  //        List<Propuesta> propuestas = gestorAlgoritmos.ejecutar();
  //
  //        EjecucionAsignacionDTO ejecucion = new EjecucionAsignacionDTO();
  //        ejecucion.setFechaEjecucion(LocalDateTime.now());
  //        ejecucion.setCantidadPropuestasGeneradas(propuestas.size());
  //
  //        asignacionRepository.save(null, ejecucion);
  //
  //        return propuestas;

}
