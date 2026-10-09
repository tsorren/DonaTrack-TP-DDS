package grupo5.logistica.infrastructure.persistencia.mappers;

import grupo5.logistica.infrastructure.persistencia.entities.CambioEstadoChoferEntity;
import grupo5.logistica.infrastructure.persistencia.entities.ChoferEntity;
import grupo5.logistica.models.entities.choferes.CambioEstadoChofer;
import grupo5.logistica.models.entities.choferes.Chofer;
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
public class ChoferPersistenciaMapper {

  public ChoferEntity toEntity(Chofer domain) {
    return toEntity(domain, null);
  }

  public ChoferEntity toEntity(Chofer domain, ChoferEntity existing) {
    if (domain == null) {
      return null;
    }
    if (existing != null
        && domain.getVersion() != null
        && !Objects.equals(domain.getVersion(), existing.getVersion())) {
      throw new ObjectOptimisticLockingFailureException(ChoferEntity.class, domain.getId());
    }

    ChoferEntity entity = existing != null ? existing : new ChoferEntity();
    entity.setIdChofer(domain.getId());
    entity.setNombre(domain.getNombre());
    entity.setApellido(domain.getApellido());
    entity.setLicencia(domain.getLicencia());
    entity.setTelefono(domain.getTelefonoContacto());
    entity.setEstadoChofer(domain.getEstado());
    if (existing == null && domain.getVersion() != null) {
      entity.setVersion(domain.getVersion());
    }

    sincronizarHistorial(domain, entity);
    return entity;
  }

  public Chofer toDomain(ChoferEntity entity) {
    return toDomain(entity, null);
  }

  public Chofer toDomain(ChoferEntity entity, UUID rutaActivaId) {
    if (entity == null) {
      return null;
    }

    List<CambioEstadoChofer> historial =
        entity.getHistorialEstados() == null
            ? List.of()
            : entity.getHistorialEstados().stream()
                .map(
                    c ->
                        new CambioEstadoChofer(
                            c.getEstadoAnterior(),
                            c.getEstadoNuevo(),
                            aLocalDateTimeUtc(c.getTimestamp())))
                .toList();

    return new Chofer(
        entity.getIdChofer(),
        entity.getNombre(),
        entity.getApellido(),
        entity.getLicencia(),
        entity.getTelefono(),
        entity.getEstadoChofer(),
        rutaActivaId,
        historial,
        entity.getVersion());
  }

  private static void sincronizarHistorial(Chofer domain, ChoferEntity entity) {
    List<CambioEstadoChoferEntity> historialEntity = entity.getHistorialEstados();
    if (historialEntity == null) {
      historialEntity = new ArrayList<>();
      entity.setHistorialEstados(historialEntity);
    }

    List<CambioEstadoChofer> historialDominio = domain.getHistorialEstados();
    for (int i = historialEntity.size(); i < historialDominio.size(); i++) {
      CambioEstadoChofer cambio = historialDominio.get(i);
      CambioEstadoChoferEntity nuevo = new CambioEstadoChoferEntity();
      nuevo.setIdCambioEstadoChofer(UUID.randomUUID());
      nuevo.setChofer(entity);
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
