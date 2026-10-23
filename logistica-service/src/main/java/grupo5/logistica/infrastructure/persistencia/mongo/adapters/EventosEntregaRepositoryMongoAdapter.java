package grupo5.logistica.infrastructure.persistencia.mongo.adapters; // NOSONAR

import grupo5.logistica.infrastructure.persistencia.jpa.entities.TipoEventoEntrega;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.EventoEntregaDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.repositories.SpringDataEventoEntregaMongoRepository;
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

@SuppressWarnings("java:S120")
@Repository
@Profile("mongo")
public class EventosEntregaRepositoryMongoAdapter implements IEventosEntregaRepository {

  private final SpringDataEventoEntregaMongoRepository springDataRepo;

  public EventosEntregaRepositoryMongoAdapter(
      SpringDataEventoEntregaMongoRepository springDataRepo) {
    this.springDataRepo = springDataRepo;
  }

  @Override
  public void registrarRutaAsignada(EventoRutaAsignada evento, Entrega entrega) {
    if (evento == null || entrega == null) {
      return;
    }
    EventoEntregaDocument doc = new EventoEntregaDocument();
    doc.setId(evento.getId());
    doc.setIdEntrega(evento.getEntregaId());
    doc.setIdDonacion(entrega.getIdDonacion());
    doc.setIdRuta(evento.getRutaId());
    doc.setOcurrioEn(aInstantDeEvento(evento.getTimestamp()));
    doc.setTipo(TipoEventoEntrega.RUTA_ASIGNADA);
    springDataRepo.save(doc);
  }

  @Override
  public void registrarEntregaExitosa(EntregaConfirmada evento) {
    if (evento == null) {
      return;
    }
    EventoEntregaDocument doc = new EventoEntregaDocument();
    doc.setId(evento.getId());
    doc.setIdEntrega(evento.getEntregaId());
    doc.setIdDonacion(evento.getDonacionId());
    doc.setIdRuta(evento.getIdRuta());
    doc.setOcurrioEn(aInstantDeEvento(evento.getTimestamp()));
    doc.setTipo(TipoEventoEntrega.ENTREGA_EXITOSA);
    springDataRepo.save(doc);
  }

  @Override
  public void registrarEntregaFallida(EntregaFallida evento) {
    if (evento == null) {
      return;
    }
    EventoEntregaDocument doc = new EventoEntregaDocument();
    doc.setId(evento.getId());
    doc.setIdEntrega(evento.getEntregaId());
    doc.setIdDonacion(evento.getDonacionId());
    doc.setIdRuta(null);
    doc.setOcurrioEn(aInstantDeEvento(evento.getTimestamp()));
    doc.setTipo(TipoEventoEntrega.ENTREGA_FALLIDA);
    doc.setJustificacion(evento.getJustificacion());
    doc.setReplanificable(evento.isReplanificable());
    springDataRepo.save(doc);
  }

  private static Instant aInstantDeEvento(LocalDateTime timestamp) {
    return timestamp != null ? timestamp.atZone(ZoneId.systemDefault()).toInstant() : Instant.now();
  }
}
