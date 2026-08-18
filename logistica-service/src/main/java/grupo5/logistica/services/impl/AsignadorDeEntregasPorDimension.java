package grupo5.logistica.services.impl;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.services.AlgoritmoAsignadorDeEntregas;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Implementación concreta de {@link AlgoritmoAsignadorDeEntregas} que encaja las entregas en los
 * camiones disponibles validando que la suma del peso y del volumen de las entregas asignadas a
 * cada camión no supere su capacidad individual.
 *
 * <p>Estrategia de encaje: para cada entrega (ya ordenada por el {@code
 * AlgoritmoOrdenadorDeEntrega}), se recorren los camiones en el orden recibido y se la asigna al
 * primero que tenga capacidad de peso y volumen suficiente (first-fit). Si ninguna camioneta
 * dispone de capacidad, la entrega queda sin asignar en este ciclo y permanecerá pendiente para una
 * próxima ejecución de planificación.
 *
 * <p><b>Nota de diseño:</b> la altura máxima del camión ({@code Camion#getAltura()}) es una
 * restricción física relevante mencionada en el enunciado, pero {@code Entrega} no modela
 * actualmente una altura por bien/entrega. Por lo tanto, hoy sólo se validan peso y volumen; la
 * validación de altura queda como una extensión natural una vez que el dominio incorpore ese dato.
 */
@Component
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
    for (Camion camion : camiones) {
      capacidadRestante.put(
          camion, new float[] {camion.getCapacidadKG(), camion.getCapacidadVolumen()});
    }

    for (Entrega entrega : entregas) {
      Camion camionElegido = buscarCamionConCapacidad(entrega, camiones, capacidadRestante);
      if (camionElegido == null) {
        continue;
      }
      float[] restante = capacidadRestante.get(camionElegido);
      restante[0] -= entrega.getPesoTotalKG();
      restante[1] -= entrega.getVolumenTotalM3();
      asignacion.computeIfAbsent(camionElegido, c -> new java.util.ArrayList<>()).add(entrega);
    }

    return asignacion;
  }

  private static Camion buscarCamionConCapacidad(
      Entrega entrega, List<Camion> camiones, Map<Camion, float[]> capacidadRestante) {
    for (Camion camion : camiones) {
      float[] restante = capacidadRestante.get(camion);
      if (restante[0] >= entrega.getPesoTotalKG() && restante[1] >= entrega.getVolumenTotalM3()) {
        return camion;
      }
    }
    return null;
  }
}
