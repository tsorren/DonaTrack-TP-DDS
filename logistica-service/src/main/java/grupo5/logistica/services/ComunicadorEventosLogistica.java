package grupo5.logistica.services;

import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.eventos.EntregaConfirmada;
import grupo5.logistica.models.entities.entregas.eventos.EntregaFallida;
import grupo5.logistica.models.entities.rutas.Ruta;
import java.util.List;

public interface ComunicadorEventosLogistica {

  void comunicarRutaAsignada(Ruta ruta, Entrega entrega);

  void comunicarRutaIniciada(Ruta ruta, Camion camion, List<Entrega> entregas);

  void comunicarEntregaExitosa(EntregaConfirmada evento, Camion camion);

  void comunicarEntregaFallida(EntregaFallida evento);
}
