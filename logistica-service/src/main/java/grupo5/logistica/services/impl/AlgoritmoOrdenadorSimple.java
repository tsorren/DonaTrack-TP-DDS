package grupo5.logistica.services.impl;

import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.services.AlgoritmoOrdenadorDeEntregas;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AlgoritmoOrdenadorSimple implements AlgoritmoOrdenadorDeEntregas {

  @Override
  public List<Entrega> obtenerEntregasOrdenadas(List<Entrega> entregas) {
    return new ArrayList<>(entregas);
  }
}
