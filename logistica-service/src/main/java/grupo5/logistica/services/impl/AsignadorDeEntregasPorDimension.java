package grupo5.logistica.services.impl;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.services.AlgoritmoAsignadorDeEntregas;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AsignadorDeEntregasPorDimension implements AlgoritmoAsignadorDeEntregas {

  @Override
  public Map<UUID, List<Entrega>> asignar(List<Entrega> entregas, List<Camion> camiones) {
    if (entregas == null || camiones == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }

    Map<UUID, List<Entrega>> asignaciones = new LinkedHashMap<>();
    Map<UUID, Float> pesoDisponiblePorCamion = new LinkedHashMap<>();
    Map<UUID, Float> volumenDisponiblePorCamion = new LinkedHashMap<>();

    camiones.forEach(
        camion -> {
          asignaciones.put(camion.getId(), new ArrayList<>());
          pesoDisponiblePorCamion.put(camion.getId(), camion.getCapacidadKG());
          volumenDisponiblePorCamion.put(camion.getId(), camion.getCapacidadVolumen());
        });

    for (Entrega entrega : entregas) {
      UUID camionId =
          buscarCamionDisponible(entrega, pesoDisponiblePorCamion, volumenDisponiblePorCamion);
      if (camionId != null) {
        asignaciones.get(camionId).add(entrega);
        pesoDisponiblePorCamion.put(
            camionId, pesoDisponiblePorCamion.get(camionId) - entrega.getPesoTotalKG());
        volumenDisponiblePorCamion.put(
            camionId, volumenDisponiblePorCamion.get(camionId) - entrega.getVolumenTotalM3());
      }
    }

    return asignaciones;
  }

  private static UUID buscarCamionDisponible(
      Entrega entrega,
      Map<UUID, Float> pesoDisponiblePorCamion,
      Map<UUID, Float> volumenDisponiblePorCamion) {
    return pesoDisponiblePorCamion.entrySet().stream()
        .filter(entry -> entry.getValue() >= entrega.getPesoTotalKG())
        .filter(
            entry -> volumenDisponiblePorCamion.get(entry.getKey()) >= entrega.getVolumenTotalM3())
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse(null);
  }
}
