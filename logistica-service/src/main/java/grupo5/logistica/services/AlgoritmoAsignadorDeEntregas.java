package grupo5.logistica.services;

import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.entregas.Entrega;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AlgoritmoAsignadorDeEntregas {
  Map<UUID, List<Entrega>> asignar(List<Entrega> entregas, List<Camion> camiones);
}
