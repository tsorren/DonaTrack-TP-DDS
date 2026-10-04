package grupo5.logistica.infrastructure;

import grupo5.logistica.dto.eventos.EventoEntregaExitosa;
import grupo5.logistica.dto.eventos.EventoEntregaFallida;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.eventos.EntregaConfirmada;
import grupo5.logistica.models.entities.entregas.eventos.EntregaFallida;
import grupo5.logistica.models.entities.rutas.eventos.EventoRutaAsignada;
import grupo5.logistica.models.entities.rutas.eventos.EventoRutaIniciada;
import grupo5.logistica.models.repositories.IEventosEntregaRepository;
import grupo5.logistica.services.ComunicadorEventosLogistica;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class ComunicadorEventosLogisticaRabbit implements ComunicadorEventosLogistica {

  private final LogisticaEventPublisher eventPublisher;
  private final GeneradorDeURLSeguimiento generadorDeUrlSeguimiento;
  private final IEventosEntregaRepository eventosEntregaRepository;

  public ComunicadorEventosLogisticaRabbit(
      LogisticaEventPublisher eventPublisher, GeneradorDeURLSeguimiento generadorDeUrlSeguimiento) {
    this(eventPublisher, generadorDeUrlSeguimiento, null);
  }

  @Autowired
  public ComunicadorEventosLogisticaRabbit(
      LogisticaEventPublisher eventPublisher,
      GeneradorDeURLSeguimiento generadorDeUrlSeguimiento,
      @Nullable IEventosEntregaRepository eventosEntregaRepository) {
    this.eventPublisher = eventPublisher;
    this.generadorDeUrlSeguimiento = generadorDeUrlSeguimiento;
    this.eventosEntregaRepository = eventosEntregaRepository;
  }

  @Override
  public void comunicarRutaAsignada(EventoRutaAsignada evento, Entrega entrega) {
    if (eventosEntregaRepository != null) {
      eventosEntregaRepository.registrarRutaAsignada(evento, entrega);
    }
    eventPublisher.publicarRutaAsignada(
        new grupo5.logistica.dto.eventos.EventoRutaAsignada(
            evento.getRutaId(), entrega.getIdDonacion(), evento.getTimestamp()));
  }

  @Override
  public void comunicarRutaIniciada(
      EventoRutaIniciada evento, Camion camion, List<Entrega> entregas) {
    List<UUID> donacionesIds = entregas.stream().map(Entrega::getIdDonacion).toList();
    eventPublisher.publicarRutaIniciada(
        new grupo5.logistica.dto.eventos.EventoRutaIniciada(
            evento.getRutaId(),
            camion.getId(),
            camion.getPatente(),
            donacionesIds,
            evento.getFechaInicio(),
            generadorDeUrlSeguimiento.generarUrl(evento.getRutaId())));
  }

  @Override
  public void comunicarEntregaExitosa(EntregaConfirmada evento, Camion camion) {
    if (eventosEntregaRepository != null) {
      eventosEntregaRepository.registrarEntregaExitosa(evento);
    }
    eventPublisher.publicarEntregaExitosa(
        new EventoEntregaExitosa(
            evento.getEntregaId(),
            evento.getDonacionId(),
            camion.getId(),
            camion.getPatente(),
            evento.getTimestamp()));
  }

  @Override
  public void comunicarEntregaFallida(EntregaFallida evento) {
    if (eventosEntregaRepository != null) {
      eventosEntregaRepository.registrarEntregaFallida(evento);
    }
    eventPublisher.publicarEntregaFallida(
        new EventoEntregaFallida(
            evento.getEntregaId(),
            evento.getDonacionId(),
            evento.getJustificacion(),
            evento.getTimestamp(),
            evento.isReplanificable()));
  }
}
