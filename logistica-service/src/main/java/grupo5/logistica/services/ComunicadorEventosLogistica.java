package grupo5.logistica.services;

import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.NoRecepcion;
import grupo5.logistica.models.entities.rutas.Ruta;
import java.util.List;

public interface ComunicadorEventosLogistica {

  void comunicarRutaAsignada(Ruta ruta, Entrega entrega);

  void comunicarRutaIniciada(Ruta ruta, Camion camion, List<Entrega> entregas);

  void comunicarEntregaExitosa(Entrega entrega, Camion camion);

  void comunicarEntregaFallida(NoRecepcion solicitud);
}
