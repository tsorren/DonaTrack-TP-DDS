package grupo5.logistica.infrastructure.persistencia.mappers;

import grupo5.logistica.infrastructure.persistencia.entities.SolicitudPlanificacionEntity;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Component
public class SolicitudPlanificacionPersistenciaMapper {

  public SolicitudPlanificacionEntity toEntity(SolicitudPlanificacion domain) {
    return toEntity(domain, null);
  }

  public SolicitudPlanificacionEntity toEntity(
      SolicitudPlanificacion domain, SolicitudPlanificacionEntity existing) {
    if (domain == null) {
      return null;
    }
    if (existing != null
        && domain.getVersion() != null
        && !Objects.equals(domain.getVersion(), existing.getVersion())) {
      throw new ObjectOptimisticLockingFailureException(
          SolicitudPlanificacionEntity.class, domain.getId());
    }

    SolicitudPlanificacionEntity entity =
        existing != null ? existing : new SolicitudPlanificacionEntity();
    entity.setIdSolicitudPlanificacion(domain.getId());
    entity.setFecha(domain.getFecha());
    entity.setEstado(domain.getEstado());
    entity.setCantidadDonaciones(domain.getCantidadDonaciones());
    entity.setCallbackUrl(domain.getCallbackUrl());
    entity.setIntentosFallidos(domain.getIntentosFallidos());
    entity.setMotivoError(domain.getMotivoError());
    Set<UUID> rutasGeneradas = entity.getRutasGeneradas();
    if (rutasGeneradas == null) {
      entity.setRutasGeneradas(new LinkedHashSet<>(domain.getRutasGeneradas()));
    } else {
      rutasGeneradas.retainAll(domain.getRutasGeneradas());
      rutasGeneradas.addAll(domain.getRutasGeneradas());
    }
    if (existing == null && domain.getVersion() != null) {
      entity.setVersion(domain.getVersion());
    }

    return entity;
  }

  public SolicitudPlanificacion toDomain(SolicitudPlanificacionEntity entity) {
    if (entity == null) {
      return null;
    }

    return new SolicitudPlanificacion(
        entity.getIdSolicitudPlanificacion(),
        entity.getFecha(),
        entity.getEstado(),
        entity.getCantidadDonaciones(),
        entity.getCallbackUrl(),
        entity.getRutasGeneradas() != null
            ? new ArrayList<>(entity.getRutasGeneradas())
            : List.of(),
        entity.getIntentosFallidos(),
        entity.getMotivoError(),
        entity.getVersion());
  }
}
