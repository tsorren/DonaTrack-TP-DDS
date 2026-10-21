package grupo5.logistica.models.repositories;

import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.eventos.EntregaConfirmada;
import grupo5.logistica.models.entities.entregas.eventos.EntregaFallida;
import grupo5.logistica.models.entities.rutas.eventos.EventoRutaAsignada;

public interface IEventosEntregaRepository {
  void registrarRutaAsignada(EventoRutaAsignada evento, Entrega entrega);

  void registrarEntregaExitosa(EntregaConfirmada evento);

  void registrarEntregaFallida(EntregaFallida evento);
}
