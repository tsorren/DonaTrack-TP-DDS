package grupo5.logistica.infrastructure.persistencia.mappers;

import grupo5.logistica.infrastructure.persistencia.entities.CambioEstadoCamionEntity;
import grupo5.logistica.infrastructure.persistencia.entities.CamionEntity;
import grupo5.logistica.models.entities.camiones.CambioEstadoCamion;
import grupo5.logistica.models.entities.camiones.Camion;
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
public class CamionPersistenciaMapper {

  public CamionEntity toEntity(Camion domain) {
    return toEntity(domain, null);
  }

  public CamionEntity toEntity(Camion domain, CamionEntity existing) {
    if (domain == null) {
      return null;
    }
    if (existing != null
        && domain.getVersion() != null
        && !Objects.equals(domain.getVersion(), existing.getVersion())) {
      throw new ObjectOptimisticLockingFailureException(CamionEntity.class, domain.getId());
    }

    CamionEntity entity = existing != null ? existing : new CamionEntity();
    entity.setIdCamion(domain.getId());
    entity.setPatente(domain.getPatente());
    entity.setCapacidadVolumen(aDouble(domain.getCapacidadVolumen()));
    entity.setCapacidadPeso(aDouble(domain.getCapacidadKG()));
    entity.setAltura(aDouble(domain.getAltura()));
    entity.setEstadoCamion(domain.getEstado());
    if (existing == null && domain.getVersion() != null) {
      entity.setVersion(domain.getVersion());
    }

    sincronizarHistorial(domain, entity);
    return entity;
  }

  public Camion toDomain(CamionEntity entity) {
    return toDomain(entity, null);
  }

  public Camion toDomain(CamionEntity entity, UUID rutaActivaId) {
    if (entity == null) {
      return null;
    }

    List<CambioEstadoCamion> historial =
        entity.getHistorialEstado() == null
            ? List.of()
            : entity.getHistorialEstado().stream()
                .map(
                    c ->
                        new CambioEstadoCamion(
                            c.getEstadoAnterior(),
                            c.getEstadoNuevo(),
                            aLocalDateTimeUtc(c.getTimestamp())))
                .toList();

    return new Camion(
        entity.getIdCamion(),
        rutaActivaId,
        entity.getPatente(),
        aFloat(entity.getCapacidadVolumen()),
        aFloat(entity.getCapacidadPeso()),
        aFloat(entity.getAltura()),
        entity.getEstadoCamion(),
        historial,
        entity.getVersion());
  }

  private static void sincronizarHistorial(Camion domain, CamionEntity entity) {
    List<CambioEstadoCamionEntity> historialEntity = entity.getHistorialEstado();
    if (historialEntity == null) {
      historialEntity = new ArrayList<>();
      entity.setHistorialEstado(historialEntity);
    }

    List<CambioEstadoCamion> historialDominio = domain.getHistorialEstado();
    for (int i = historialEntity.size(); i < historialDominio.size(); i++) {
      CambioEstadoCamion cambio = historialDominio.get(i);
      CambioEstadoCamionEntity nuevo = new CambioEstadoCamionEntity();
      nuevo.setIdCambioEstadoCamion(UUID.randomUUID());
      nuevo.setCamion(entity);
      nuevo.setEstadoAnterior(cambio.estadoAnterior());
      nuevo.setEstadoNuevo(cambio.estadoNuevo());
      nuevo.setTimestamp(aInstantUtc(cambio.timestamp()));
      historialEntity.add(nuevo);
    }
  }

  private static Double aDouble(Float valor) {
    return valor != null ? valor.doubleValue() : null;
  }

  private static Float aFloat(Double valor) {
    return valor != null ? valor.floatValue() : null;
  }

  private static Instant aInstantUtc(LocalDateTime fechaHora) {
    return fechaHora != null ? fechaHora.toInstant(ZoneOffset.UTC) : null;
  }

  private static LocalDateTime aLocalDateTimeUtc(Instant instante) {
    return instante != null ? LocalDateTime.ofInstant(instante, ZoneOffset.UTC) : null;
  }
}
