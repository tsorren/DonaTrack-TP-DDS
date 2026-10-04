package grupo5.logistica.infrastructure.persistencia.mappers;

import grupo5.logistica.infrastructure.persistencia.entities.CambioEstadoRutaEntity;
import grupo5.logistica.infrastructure.persistencia.entities.RutaEntity;
import grupo5.logistica.models.entities.rutas.CambioEstadoRuta;
import grupo5.logistica.models.entities.rutas.Ruta;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Component
public class RutaPersistenciaMapper {

  public RutaEntity toEntity(Ruta domain) {
    return toEntity(domain, null);
  }

  public RutaEntity toEntity(Ruta domain, RutaEntity existing) {
    if (domain == null) {
      return null;
    }
    if (existing != null
        && domain.getVersion() != null
        && !Objects.equals(domain.getVersion(), existing.getVersion())) {
      throw new ObjectOptimisticLockingFailureException(RutaEntity.class, domain.getId());
    }

    RutaEntity entity = existing != null ? existing : new RutaEntity();
    entity.setIdRuta(domain.getId());
    entity.setIdChofer(domain.getChoferId());
    entity.setIdCamion(domain.getCamionId());
    entity.setFecha(domain.getFecha());
    entity.setEstado(domain.getEstado());
    entity.setHoraInicioReal(aInstantUtc(domain.getHoraInicioReal()));
    entity.setHoraFinReal(aInstantUtc(domain.getHoraFinReal()));
    if (existing == null && domain.getVersion() != null) {
      entity.setVersion(domain.getVersion());
    }

    sincronizarHistorial(domain, entity);
    return entity;
  }

  public Ruta toDomain(RutaEntity entity) {
    return toDomain(entity, List.of());
  }

  public Ruta toDomain(RutaEntity entity, List<UUID> entregaIds) {
    if (entity == null) {
      return null;
    }

    List<CambioEstadoRuta> historial =
        entity.getHistorialEstado() == null
            ? List.of()
            : entity.getHistorialEstado().stream()
                .map(
                    c ->
                        new CambioEstadoRuta(
                            c.getEstadoAnterior(),
                            c.getEstadoNuevo(),
                            aLocalDateTimeUtc(c.getTimestamp())))
                .toList();

    return new Ruta(
        entity.getIdRuta(),
        entity.getFecha(),
        entregaIds != null ? entregaIds : List.of(),
        entity.getIdChofer(),
        entity.getIdCamion(),
        entity.getEstado(),
        historial,
        aLocalDateTimeUtc(entity.getHoraInicioReal()),
        aLocalDateTimeUtc(entity.getHoraFinReal()),
        entity.getVersion());
  }

  private static void sincronizarHistorial(Ruta domain, RutaEntity entity) {
    List<CambioEstadoRutaEntity> historialEntity = entity.getHistorialEstado();
    if (historialEntity == null) {
      historialEntity = new ArrayList<>();
      entity.setHistorialEstado(historialEntity);
    }

    List<CambioEstadoRuta> historialDominio = domain.getHistorialEstado();
    for (int i = historialEntity.size(); i < historialDominio.size(); i++) {
      CambioEstadoRuta cambio = historialDominio.get(i);
      CambioEstadoRutaEntity nuevo = new CambioEstadoRutaEntity();
      nuevo.setIdCambioEstadoRuta(UUID.randomUUID());
      nuevo.setRuta(entity);
      nuevo.setEstadoAnterior(cambio.estadoAnterior());
      nuevo.setEstadoNuevo(cambio.estadoNuevo());
      nuevo.setTimestamp(aInstantUtc(cambio.timestamp()));
      historialEntity.add(nuevo);
    }
  }

  private static Instant aInstantUtc(LocalDateTime fechaHora) {
    return fechaHora != null ? fechaHora.toInstant(ZoneOffset.UTC) : null;
  }

  private static LocalDateTime aLocalDateTimeUtc(Instant instante) {
    return instante != null ? LocalDateTime.ofInstant(instante, ZoneOffset.UTC) : null;
  }
}
