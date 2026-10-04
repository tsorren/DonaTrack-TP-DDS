package grupo5.logistica.infrastructure.persistencia.mappers;

import grupo5.logistica.infrastructure.persistencia.entities.CambioEstadoEntregaEntity;
import grupo5.logistica.infrastructure.persistencia.entities.DireccionEntity;
import grupo5.logistica.infrastructure.persistencia.entities.EntregaEntity;
import grupo5.logistica.infrastructure.persistencia.entities.LocalidadEntity;
import grupo5.logistica.infrastructure.persistencia.entities.PaisEntity;
import grupo5.logistica.infrastructure.persistencia.entities.ProvinciaEntity;
import grupo5.logistica.models.entities.entregas.CambioEstadoEntrega;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.rutas.direccion.Direccion;
import grupo5.logistica.models.entities.rutas.direccion.Localidad;
import grupo5.logistica.models.entities.rutas.direccion.Pais;
import grupo5.logistica.models.entities.rutas.direccion.Provincia;
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
public class EntregaPersistenciaMapper {

  public EntregaEntity toEntity(Entrega domain) {
    if (domain == null) {
      return null;
    }
    return toEntity(domain, null, construirDireccionNueva(domain.getDestino(), null));
  }

  public EntregaEntity toEntity(
      Entrega domain, EntregaEntity existing, DireccionEntity direccionParaCreacion) {
    if (domain == null) {
      return null;
    }
    if (existing != null
        && domain.getVersion() != null
        && !Objects.equals(domain.getVersion(), existing.getVersion())) {
      throw new ObjectOptimisticLockingFailureException(EntregaEntity.class, domain.getId());
    }

    EntregaEntity entity = existing != null ? existing : new EntregaEntity();
    entity.setIdEntrega(domain.getId());
    entity.setIdRuta(domain.getIdRuta());
    entity.setIdDonacion(domain.getIdDonacion());
    entity.setIdBeneficiario(domain.getIdBeneficiaria());
    entity.setEstado(domain.getEstadoActual());
    entity.setHoraArribo(aInstantUtc(domain.getHoraArribo()));
    entity.setHoraSalida(aInstantUtc(domain.getHoraSalida()));
    entity.setFotoRecepcionUrl(domain.getFotoRecepcionUrl());
    entity.setVolumenTotalM3((double) domain.getVolumenTotalM3());
    entity.setPesoTotalKg((double) domain.getPesoTotalKG());

    if (existing == null || existing.getDireccion() == null) {
      entity.setDireccion(direccionParaCreacion);
    }
    if (existing == null && domain.getVersion() != null) {
      entity.setVersion(domain.getVersion());
    }

    sincronizarHistorial(domain, entity);
    return entity;
  }

  public DireccionEntity construirDireccionNueva(
      Direccion destino, LocalidadEntity localidadEntity) {
    if (destino == null) {
      return null;
    }
    DireccionEntity dir = new DireccionEntity();
    dir.setIdDireccion(UUID.randomUUID());
    dir.setCalle(destino.calle());
    dir.setAltura(destino.altura());
    dir.setPiso(destino.piso() != null ? destino.piso().shortValue() : null);
    dir.setDepartamento(destino.departamento());
    dir.setCodigoPostal(destino.codigoPostal());
    dir.setLocalidad(localidadEntity);
    return dir;
  }

  public Entrega toDomain(EntregaEntity entity) {
    if (entity == null) {
      return null;
    }

    List<CambioEstadoEntrega> historial =
        entity.getHistorialEstado() == null
            ? List.of()
            : entity.getHistorialEstado().stream()
                .map(
                    c ->
                        new CambioEstadoEntrega(
                            c.getEstadoAnterior(),
                            c.getEstadoNuevo(),
                            aLocalDateTimeUtc(c.getTimestamp()),
                            c.getActor()))
                .toList();

    return new Entrega(
        entity.getIdEntrega(),
        entity.getIdRuta(),
        entity.getIdDonacion(),
        entity.getIdBeneficiario(),
        mapearDireccion(entity.getDireccion()),
        entity.getEstado(),
        historial,
        aLocalDateTimeUtc(entity.getHoraArribo()),
        aLocalDateTimeUtc(entity.getHoraSalida()),
        entity.getFotoRecepcionUrl(),
        entity.getPesoTotalKg() != null ? entity.getPesoTotalKg().floatValue() : 0f,
        entity.getVolumenTotalM3() != null ? entity.getVolumenTotalM3().floatValue() : 0f,
        entity.getVersion());
  }

  private static Direccion mapearDireccion(DireccionEntity dirEntity) {
    if (dirEntity == null) {
      return null;
    }
    LocalidadEntity locEntity = dirEntity.getLocalidad();
    ProvinciaEntity provEntity = locEntity != null ? locEntity.getProvincia() : null;
    PaisEntity paisEntity = provEntity != null ? provEntity.getPais() : null;

    Pais pais = new Pais(paisEntity != null ? paisEntity.getNombre() : "");
    Provincia provincia = new Provincia(provEntity != null ? provEntity.getNombre() : "", pais);
    Localidad localidad = new Localidad(locEntity != null ? locEntity.getNombre() : "", provincia);

    return new Direccion(
        dirEntity.getCalle(),
        dirEntity.getAltura(),
        dirEntity.getPiso() != null ? dirEntity.getPiso().intValue() : null,
        dirEntity.getDepartamento(),
        dirEntity.getCodigoPostal(),
        localidad);
  }

  private static void sincronizarHistorial(Entrega domain, EntregaEntity entity) {
    List<CambioEstadoEntregaEntity> historialEntity = entity.getHistorialEstado();
    if (historialEntity == null) {
      historialEntity = new ArrayList<>();
      entity.setHistorialEstado(historialEntity);
    }

    List<CambioEstadoEntrega> historialDominio = domain.getHistorialEstado();
    for (int i = historialEntity.size(); i < historialDominio.size(); i++) {
      CambioEstadoEntrega cambio = historialDominio.get(i);
      CambioEstadoEntregaEntity nuevo = new CambioEstadoEntregaEntity();
      nuevo.setIdCambioEstadoEntrega(UUID.randomUUID());
      nuevo.setEntrega(entity);
      nuevo.setEstadoAnterior(cambio.estadoAnterior());
      nuevo.setEstadoNuevo(cambio.estadoNuevo());
      nuevo.setTimestamp(aInstantUtc(cambio.timeStamp()));
      nuevo.setActor(cambio.actor());
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
