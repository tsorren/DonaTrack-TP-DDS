package grupo5.logistica.models.entities.planificacion;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.entregas.Entrega;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AsignadorDeEntregasPorDimension implements AlgoritmoAsignadorDeEntregas {

  @Override
  public Map<Camion, List<Entrega>> asignar(List<Entrega> entregas, List<Camion> camiones) {
    if (entregas == null) {
      throw new ValidationException(ErrorCatalog.GENERADOR_RUTAS_ENTREGAS_NULAS);
    }
    if (camiones == null) {
      throw new ValidationException(ErrorCatalog.GENERADOR_RUTAS_CAMIONES_NULOS);
    }

    Map<Camion, List<Entrega>> asignacion = new LinkedHashMap<>();
    Map<Camion, float[]> capacidadRestante = new LinkedHashMap<>();
    camiones.forEach(
        camion ->
            capacidadRestante.put(
                camion, new float[] {camion.getCapacidadKG(), camion.getCapacidadVolumen()}));

    for (Entrega entrega : entregas) {
      Camion camion = buscarCamionConCapacidad(entrega, camiones, capacidadRestante);
      if (camion == null) {
        continue;
      }
      float[] restante = capacidadRestante.get(camion);
      restante[0] -= entrega.getPesoTotalKG();
      restante[1] -= entrega.getVolumenTotalM3();
      asignacion.computeIfAbsent(camion, ignored -> new ArrayList<>()).add(entrega);
    }
    return asignacion;
  }

  private static Camion buscarCamionConCapacidad(
      Entrega entrega, List<Camion> camiones, Map<Camion, float[]> capacidadRestante) {
    return camiones.stream()
        .filter(
            camion -> {
              float[] restante = capacidadRestante.get(camion);
              return restante[0] >= entrega.getPesoTotalKG()
                  && restante[1] >= entrega.getVolumenTotalM3();
            })
        .findFirst()
        .orElse(null);
  }
}
