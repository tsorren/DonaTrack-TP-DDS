package grupo5.logistica.infrastructure.persistencia.mongo.adapters; // NOSONAR

import grupo5.logistica.infrastructure.persistencia.mongo.documents.EventoEntregaDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.TipoEventoEntrega;
import grupo5.logistica.infrastructure.persistencia.mongo.repositories.SpringDataEventoEntregaMongoRepository;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.eventos.EntregaConfirmada;
import grupo5.logistica.models.entities.entregas.eventos.EntregaFallida;
import grupo5.logistica.models.entities.rutas.eventos.EventoRutaAsignada;
import grupo5.logistica.models.repositories.IEventosEntregaRepository;
import java.time.Clock;
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
  private final Clock reloj;

  public EventosEntregaRepositoryMongoAdapter(
      SpringDataEventoEntregaMongoRepository springDataRepo, Clock reloj) {
    this.springDataRepo = springDataRepo;
    this.reloj = reloj;
  }

  @Override
  public void registrarRutaAsignada(EventoRutaAsignada evento, Entrega entrega) {
    if (evento == null || entrega == null) {
      return;
    }
    springDataRepo.save(documentoDe(evento, entrega));
  }

  @Override
  public void registrarEntregaExitosa(EntregaConfirmada evento) {
    if (evento == null) {
      return;
    }
    springDataRepo.save(documentoDe(evento));
  }

  @Override
  public void registrarEntregaFallida(EntregaFallida evento) {
    if (evento == null) {
      return;
    }
    springDataRepo.save(documentoDe(evento));
  }

  private EventoEntregaDocument documentoDe(EventoRutaAsignada evento, Entrega entrega) {
    return EventoEntregaDocument.builder()
        .id(evento.getId())
        .idEntrega(evento.getEntregaId())
        .idDonacion(entrega.getIdDonacion())
        .idRuta(evento.getRutaId())
        .ocurrioEn(aInstantDeEvento(evento.getTimestamp()))
        .tipo(TipoEventoEntrega.RUTA_ASIGNADA)
        .build();
  }

  private EventoEntregaDocument documentoDe(EntregaConfirmada evento) {
    return EventoEntregaDocument.builder()
        .id(evento.getId())
        .idEntrega(evento.getEntregaId())
        .idDonacion(evento.getDonacionId())
        .idRuta(evento.getIdRuta())
        .ocurrioEn(aInstantDeEvento(evento.getTimestamp()))
        .tipo(TipoEventoEntrega.ENTREGA_EXITOSA)
        .build();
  }

  private EventoEntregaDocument documentoDe(EntregaFallida evento) {
    return EventoEntregaDocument.builder()
        .id(evento.getId())
        .idEntrega(evento.getEntregaId())
        .idDonacion(evento.getDonacionId())
        .ocurrioEn(aInstantDeEvento(evento.getTimestamp()))
        .tipo(TipoEventoEntrega.ENTREGA_FALLIDA)
        .justificacion(evento.getJustificacion())
        .replanificable(evento.isReplanificable())
        .build();
  }

  /**
   * {@code EventoDeDominio} arma el timestamp con la zona del sistema, así que se convierte con esa
   * misma zona y no con {@code FechasUtc}: leerlo como UTC correría el instante.
   */
  private Instant aInstantDeEvento(LocalDateTime timestamp) {
    if (timestamp == null) {
      return Instant.now(reloj);
    }
    return timestamp.atZone(ZoneId.systemDefault()).toInstant();
  }
}
