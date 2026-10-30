package grupo5.donaciones.infrastructure.persistencia.mapper;

import grupo5.donaciones.dto.logistica.DatosEntregaLogistica;
import grupo5.donaciones.infrastructure.idempotency.EventoConsumido;
import grupo5.donaciones.infrastructure.persistencia.entities.logistica.EventoConsumidoEntity;
import grupo5.donaciones.infrastructure.persistencia.entities.logistica.OutboxLogisticaEntity;
import grupo5.donaciones.infrastructure.persistencia.entities.logistica.SolicitudEntregaEntity;
import grupo5.donaciones.models.entities.logistica.SolicitudEntrega;
import grupo5.donaciones.services.logistica.EntradaOutboxLogistica;
import grupo5.donaciones.services.logistica.RegistroEntradaOutbox;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Solicitud de entrega, entradas del outbox y eventos consumidos (Paso 10). */
@Component
@Profile("postgres")
public class LogisticaPersistenciaMapper {

  // Propio y no el del contexto: el payload tiene que leerse igual aunque cambie la configuración
  // de los endpoints REST
  private static final JsonMapper JSON = JsonMapper.builder().build();

  public SolicitudEntregaEntity toEntity(SolicitudEntrega domain) {
    SolicitudEntregaEntity entity = new SolicitudEntregaEntity();
    entity.setId(domain.getId());
    entity.setDonacionIndependienteId(domain.getDonacionIndependienteId());
    entity.setProveedorActual(domain.getProveedorActual());
    entity.setEstado(domain.getEstado());
    entity.setRonda(domain.getRonda());
    entity.setFechaCreacion(domain.getFechaCreacion());
    entity.setProveedoresDescartados(new ArrayList<>(domain.getProveedoresDescartados()));
    return entity;
  }

  public SolicitudEntrega toDomain(SolicitudEntregaEntity entity) {
    return new SolicitudEntrega(
        entity.getId(),
        entity.getDonacionIndependienteId(),
        entity.getFechaCreacion(),
        new LinkedHashSet<>(entity.getProveedoresDescartados()),
        entity.getProveedorActual(),
        entity.getEstado(),
        entity.getRonda());
  }

  public OutboxLogisticaEntity toEntity(EntradaOutboxLogistica domain) {
    OutboxLogisticaEntity entity = new OutboxLogisticaEntity();
    entity.setId(domain.getId());
    entity.setSolicitudId(domain.getSolicitudId());
    entity.setProveedorId(domain.getProveedorId());
    entity.setPayload(JSON.writeValueAsString(domain.getDatos()));
    entity.setTraceId(domain.getTraceId());
    entity.setProximoIntento(domain.getProximoIntento());
    entity.setEstado(domain.getEstado());
    entity.setIntentos(domain.getIntentos());
    entity.setMaxIntentos(domain.getMaxIntentos());
    return entity;
  }

  public EntradaOutboxLogistica toDomain(OutboxLogisticaEntity entity) {
    return EntradaOutboxLogistica.reconstituir(
        new RegistroEntradaOutbox(
            entity.getId(),
            entity.getSolicitudId(),
            entity.getProveedorId(),
            JSON.readValue(entity.getPayload(), DatosEntregaLogistica.class),
            entity.getTraceId(),
            entity.getMaxIntentos(),
            entity.getIntentos(),
            entity.getProximoIntento(),
            entity.getEstado()));
  }

  public EventoConsumidoEntity toEntity(EventoConsumido evento) {
    EventoConsumidoEntity entity = new EventoConsumidoEntity();
    entity.setId(evento.id());
    entity.setEventType(evento.eventType());
    entity.setQueueName(evento.queueName());
    entity.setBusinessId(evento.businessId());
    entity.setDonacionId(evento.donacionId());
    entity.setFechaProcesamiento(evento.fechaProcesamiento());
    return entity;
  }
}
