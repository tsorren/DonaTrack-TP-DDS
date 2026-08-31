package grupo5.notificaciones.infrastructure.persistencia.mappers;

import grupo5.notificaciones.infrastructure.persistencia.entities.CambioEstadoEntity;
import grupo5.notificaciones.infrastructure.persistencia.entities.NotificacionEntity;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.notificaciones.eventos.CambioEstadoNotificacion;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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

    List<CambioEstadoEntity> historial = new ArrayList<>();

    for (CambioEstadoNotificacion h : domain.getHistorialEstado()) {
      CambioEstadoEntity he = new CambioEstadoEntity();
      he.setId(UUID.randomUUID());
      he.setEstadoAnterior(h.getEstadoAnterior());
      he.setEstadoNuevo(h.getEstadoNuevo());
      he.setTimestamp(h.getTimestamp());
      historial.add(he);
    }

    entity.setHistorialEstado(historial);
    return entity;
  }

  public Notificacion toDomain(NotificacionEntity entity) {
    if (entity == null) return null;

    List<CambioEstadoNotificacion> historialDomain = new ArrayList<>();

    if (entity.getHistorialEstado() != null) {
      for (CambioEstadoEntity he : entity.getHistorialEstado()) {
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
