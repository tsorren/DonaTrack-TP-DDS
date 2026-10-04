package grupo5.logistica.models.repositories.impl;

import grupo5.common.events.EventoDeDominio;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.eventos.EntregaConfirmada;
import grupo5.logistica.models.entities.entregas.eventos.EntregaFallida;
import grupo5.logistica.models.entities.rutas.eventos.EventoRutaAsignada;
import grupo5.logistica.models.repositories.IEventosEntregaRepository;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!postgres")
public class EventosEntregaRepository implements IEventosEntregaRepository {

  private final List<EventoDeDominio> storage = new CopyOnWriteArrayList<>();

  @Override
  public void registrarRutaAsignada(EventoRutaAsignada evento, Entrega entrega) {
    if (evento != null) {
      storage.add(evento);
    }
  }

  @Override
  public void registrarEntregaExitosa(EntregaConfirmada evento) {
    if (evento != null) {
      storage.add(evento);
    }
  }

  @Override
  public void registrarEntregaFallida(EntregaFallida evento) {
    if (evento != null) {
      storage.add(evento);
    }
  }

  public List<EventoDeDominio> findAll() {
    return List.copyOf(storage);
  }

  public void deleteAll() {
    storage.clear();
  }
}
