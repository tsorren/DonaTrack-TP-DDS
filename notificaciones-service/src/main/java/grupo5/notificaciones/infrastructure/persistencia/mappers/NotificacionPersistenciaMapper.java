package grupo5.notificaciones.infrastructure.persistencia.mappers;

import grupo5.notificaciones.infrastructure.persistencia.entities.CambioEstadoEmbeddable;
import grupo5.notificaciones.infrastructure.persistencia.entities.NotificacionEntity;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.notificaciones.eventos.CambioEstadoNotificacion;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NotificacionPersistenciaMapper {

  public NotificacionEntity toEntity(Notificacion domain) {
    if (domain == null) return null;

    NotificacionEntity entity = new NotificacionEntity();
    entity.setId(domain.getId());
    entity.setPersonaId(domain.getPersonaId());
    entity.setMensaje(domain.getMensaje());
    entity.setFechaCreacion(domain.getFechaCreacion());
    entity.setEstadoNotificacion(domain.getEstadoNotificacion());

    List<CambioEstadoEmbeddable> historial = new ArrayList<>();

    for (CambioEstadoNotificacion h : domain.getHistorialEstado()) {
      historial.add(
          new CambioEstadoEmbeddable(h.getEstadoAnterior(), h.getEstadoNuevo(), h.getTimestamp()));
    }

    entity.setHistorialEstado(historial);
    return entity;
  }

  public Notificacion toDomain(NotificacionEntity entity) {
    if (entity == null) return null;

    List<CambioEstadoNotificacion> historialDomain = new ArrayList<>();

    if (entity.getHistorialEstado() != null) {
      for (CambioEstadoEmbeddable he : entity.getHistorialEstado()) {
        historialDomain.add(
            new CambioEstadoNotificacion(
                he.getEstadoAnterior(), he.getEstadoNuevo(), he.getTimestamp()));
      }
    }
    return new Notificacion(
        entity.getId(),
        entity.getPersonaId(),
        entity.getMensaje(),
        entity.getFechaCreacion(),
        entity.getEstadoNotificacion(),
        historialDomain);
  }
}
