package grupo5.donaciones.models.algoritmos;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AlgoritmoPrioridadSubAtendidos extends AlgoritmoAsignacion {

  /**
   * Primero las entidades que menos donaciones recibieron en el último trimestre. El conteo lo trae
   * {@link DatosDeAsignacion} (DTI-06: la necesidad ya no conoce sus donaciones).
   */
  @Override
  public List<Necesidad> ordenarNecesidades(List<Necesidad> necesidades, DatosDeAsignacion datos) {
    if (necesidades == null)
      throw new ValidationException(ErrorCatalog.ALGORITMO_NECESIDADES_NULAS);
    List<Necesidad> ordenadas = new ArrayList<>(necesidades);
    ordenadas.sort(Comparator.comparingInt(n -> datos.recibidasPor(n.getEntidadId())));
    return ordenadas;
  }

  @Override
  public List<DonacionIndependiente> filtrarDonaciones(
      Necesidad necesidad, List<DonacionIndependiente> donaciones) {
    return filtrarPorSubcategoria(necesidad, donaciones);
  }
}
