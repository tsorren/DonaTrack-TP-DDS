package grupo5.logistica.infrastructure.persistencia.mongo.adapters; // NOSONAR

import grupo5.logistica.infrastructure.persistencia.mongo.documents.EventoEntregaDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.TipoEventoEntrega;
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
    springDataRepo.save(
        EventoEntregaDocument.builder()
            .id(evento.getId())
            .idEntrega(evento.getEntregaId())
            .idDonacion(entrega.getIdDonacion())
            .idRuta(evento.getRutaId())
            .ocurrioEn(aInstantDeEvento(evento.getTimestamp()))
            .tipo(TipoEventoEntrega.RUTA_ASIGNADA)
            .build());
  }

  @Override
  public void registrarEntregaExitosa(EntregaConfirmada evento) {
    if (evento == null) {
      return;
    }
    springDataRepo.save(
        EventoEntregaDocument.builder()
            .id(evento.getId())
            .idEntrega(evento.getEntregaId())
            .idDonacion(evento.getDonacionId())
            .idRuta(evento.getIdRuta())
            .ocurrioEn(aInstantDeEvento(evento.getTimestamp()))
            .tipo(TipoEventoEntrega.ENTREGA_EXITOSA)
            .build());
  }

  @Override
  public void registrarEntregaFallida(EntregaFallida evento) {
    if (evento == null) {
      return;
    }
    springDataRepo.save(
        EventoEntregaDocument.builder()
            .id(evento.getId())
            .idEntrega(evento.getEntregaId())
            .idDonacion(evento.getDonacionId())
            .ocurrioEn(aInstantDeEvento(evento.getTimestamp()))
            .tipo(TipoEventoEntrega.ENTREGA_FALLIDA)
            .justificacion(evento.getJustificacion())
            .replanificable(evento.isReplanificable())
            .build());
  }

  private static Instant aInstantDeEvento(LocalDateTime timestamp) {
    return timestamp != null ? timestamp.atZone(ZoneId.systemDefault()).toInstant() : Instant.now();
  }
}
