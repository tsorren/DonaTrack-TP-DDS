package grupo5.logistica.infrastructure.persistencia.adapters;

import grupo5.logistica.infrastructure.persistencia.entities.EventoEntregaEntity;
import grupo5.logistica.infrastructure.persistencia.entities.TipoEventoEntrega;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataEventoEntregaRepository;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.eventos.EntregaConfirmada;
import grupo5.logistica.models.entities.entregas.eventos.EntregaFallida;
import grupo5.logistica.models.entities.rutas.eventos.EventoRutaAsignada;
import grupo5.logistica.models.repositories.IEventosEntregaRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("postgres")
@Transactional
public class EventosEntregaRepositoryJpaAdapter implements IEventosEntregaRepository {

  private final SpringDataEventoEntregaRepository springDataRepo;

  public EventosEntregaRepositoryJpaAdapter(SpringDataEventoEntregaRepository springDataRepo) {
    this.springDataRepo = springDataRepo;
  }

  @Override
  public void registrarRutaAsignada(EventoRutaAsignada evento, Entrega entrega) {
    if (evento == null || entrega == null) {
      return;
    }
    EventoEntregaEntity entity = new EventoEntregaEntity();
    entity.setIdEventoEntrega(evento.getId());
    entity.setIdEntrega(evento.getEntregaId());
    entity.setIdDonacion(entrega.getIdDonacion());
    entity.setIdRuta(evento.getRutaId());
    entity.setOcurrioEn(aInstantDeEvento(evento.getTimestamp()));
    entity.setTipo(TipoEventoEntrega.RUTA_ASIGNADA);
    springDataRepo.save(entity);
  }

  @Override
  public void registrarEntregaExitosa(EntregaConfirmada evento) {
    if (evento == null) {
      return;
    }
    EventoEntregaEntity entity = new EventoEntregaEntity();
    entity.setIdEventoEntrega(evento.getId());
    entity.setIdEntrega(evento.getEntregaId());
    entity.setIdDonacion(evento.getDonacionId());
    entity.setIdRuta(evento.getIdRuta());
    entity.setOcurrioEn(aInstantDeEvento(evento.getTimestamp()));
    entity.setTipo(TipoEventoEntrega.ENTREGA_EXITOSA);
    springDataRepo.save(entity);
  }

  @Override
  public void registrarEntregaFallida(EntregaFallida evento) {
    if (evento == null) {
      return;
    }
    EventoEntregaEntity entity = new EventoEntregaEntity();
    entity.setIdEventoEntrega(evento.getId());
    entity.setIdEntrega(evento.getEntregaId());
    entity.setIdDonacion(evento.getDonacionId());
    entity.setIdRuta(null);
    entity.setOcurrioEn(aInstantDeEvento(evento.getTimestamp()));
    entity.setTipo(TipoEventoEntrega.ENTREGA_FALLIDA);
    entity.setJustificacion(evento.getJustificacion());
    entity.setReplanificable(evento.isReplanificable());
    springDataRepo.save(entity);
  }

  private static Instant aInstantDeEvento(LocalDateTime timestamp) {
    return timestamp != null ? timestamp.atZone(ZoneId.systemDefault()).toInstant() : Instant.now();
  }
}
