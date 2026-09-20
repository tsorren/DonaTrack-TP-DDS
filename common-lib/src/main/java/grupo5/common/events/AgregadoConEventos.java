package grupo5.common.events;

import grupo5.common.repositories.AggregateRoot;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase base para Aggregate Roots que emiten eventos de dominio. Centraliza la acumulación,
 * snapshot defensivo inmutable y limpieza de eventos.
 *
 * @param <E> Tipo de evento de dominio gestionado por el agregado.
 */
public abstract class AgregadoConEventos<E extends EventoDeDominio> implements AggregateRoot {

  private final List<E> domainEvents = new ArrayList<>();

  public void registrarEvento(E evento) {
    if (evento != null) {
      this.domainEvents.add(evento);
    }
  }

  public List<E> getDomainEvents() {
    return List.copyOf(this.domainEvents);
  }

  public void clearDomainEvents() {
    this.domainEvents.clear();
  }
}
