package grupo5.donaciones.infrastructure.persistencia.mapper;

import grupo5.donaciones.infrastructure.persistencia.entities.necesidades.NecesidadEntity;
import grupo5.donaciones.infrastructure.persistencia.entities.necesidades.NecesidadExtraordinariaEntity;
import grupo5.donaciones.infrastructure.persistencia.entities.necesidades.NecesidadRecurrenteEntity;
import grupo5.donaciones.infrastructure.persistencia.entities.necesidades.PeriodoNecesidadEntity;
import grupo5.donaciones.models.entities.necesidades.DatosNecesidad;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.entities.necesidades.PeriodoNecesidad;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("postgres")
public class NecesidadesPersistenciaMapper {

  public NecesidadEntity toEntity(Necesidad domain) {
    return toEntity(domain, null);
  }

  public NecesidadEntity toEntity(Necesidad domain, NecesidadEntity existing) {
    if (domain == null) {
      return null;
    }

    if (domain instanceof NecesidadExtraordinaria extra) {
      NecesidadExtraordinariaEntity entity =
          (existing instanceof NecesidadExtraordinariaEntity ee)
              ? ee
              : new NecesidadExtraordinariaEntity();
      mapearCamposComunes(extra, entity);
      return entity;
    }

    if (domain instanceof NecesidadRecurrente rec) {
      NecesidadRecurrenteEntity entity =
          (existing instanceof NecesidadRecurrenteEntity re) ? re : new NecesidadRecurrenteEntity();
      mapearCamposComunes(rec, entity);

      // ISO-8601: un mes sigue siendo un mes (en días se corría) y los años no se pierden
      entity.setPeriodo(rec.getPeriodo() != null ? rec.getPeriodo().toString() : null);

      sincronizarPeriodos(rec, entity);
      return entity;
    }

    throw new IllegalArgumentException(
        "Tipo de necesidad no soportado: " + domain.getClass().getName());
  }

  public Necesidad toDomain(NecesidadEntity entity) {
    if (entity == null) {
      return null;
    }
    if (entity instanceof NecesidadExtraordinariaEntity extraEntity) {
      return new NecesidadExtraordinaria(datosComunes(extraEntity), extraEntity.isActiva());
    }
    if (entity instanceof NecesidadRecurrenteEntity recEntity) {
      return recurrenteToDomain(recEntity);
    }
    throw new IllegalArgumentException(
        "Tipo de entidad no soportado: " + entity.getClass().getName());
  }

  private static DatosNecesidad datosComunes(NecesidadEntity entity) {
    LocalDate fechaInicio =
        entity.getFechaInicio() != null
            ? entity.getFechaInicio().toLocalDate()
            : LocalDate.now(ZoneId.systemDefault());
    Integer cantidad =
        entity.getCantidadNecesitada() != null ? entity.getCantidadNecesitada().intValue() : 0;
    return new DatosNecesidad(
        entity.getId(),
        entity.getSubcategoriaId(),
        cantidad,
        entity.getDescripcion(),
        fechaInicio,
        entity.getEntidadBeneficiariaId(),
        entity.getVersion());
  }

  private static NecesidadRecurrente recurrenteToDomain(NecesidadRecurrenteEntity recEntity) {
    Period periodo =
        recEntity.getPeriodo() != null ? Period.parse(recEntity.getPeriodo()) : Period.ofMonths(1);

    List<PeriodoNecesidad> periodosDominio = new ArrayList<>();
    if (recEntity.getPeriodos() != null) {
      recEntity.getPeriodos().forEach(pe -> periodosDominio.add(periodoToDomain(pe)));
    }

    return new NecesidadRecurrente(
        datosComunes(recEntity), periodo, recEntity.isActiva(), periodosDominio);
  }

  private static PeriodoNecesidad periodoToDomain(PeriodoNecesidadEntity pe) {
    LocalDate fechaFin = pe.getFechaFin() != null ? pe.getFechaFin().toLocalDate() : null;
    Integer cantidadObjetivo =
        pe.getCantidadObjetivo() != null ? pe.getCantidadObjetivo().intValue() : 0;
    return new PeriodoNecesidad(pe.getId(), fechaFin, cantidadObjetivo);
  }

  private static void mapearCamposComunes(Necesidad domain, NecesidadEntity entity) {
    entity.setId(domain.getId());
    entity.setSubcategoriaId(domain.getSubcategoriaId());
    entity.setEntidadBeneficiariaId(domain.getEntidadId());
    entity.setCantidadNecesitada(
        domain.getCantidadNecesitada() != null ? domain.getCantidadNecesitada().longValue() : null);
    entity.setDescripcion(domain.getDescripcion());
    entity.setFechaInicio(
        domain.getFechaInicio() != null
            ? domain.getFechaInicio().atStartOfDay()
            : LocalDateTime.now(ZoneId.systemDefault()));
    entity.setActiva(domain.isActiva());
  }

  private static void sincronizarPeriodos(
      NecesidadRecurrente domain, NecesidadRecurrenteEntity entity) {
    List<PeriodoNecesidadEntity> actuales = entity.getPeriodos();
    if (actuales == null) {
      actuales = new ArrayList<>();
      entity.setPeriodos(actuales);
    }

    LocalDate cursorInicio = inicioDelProximoPeriodo(domain, actuales);
    Set<UUID> guardados =
        actuales.stream().map(PeriodoNecesidadEntity::getId).collect(Collectors.toSet());

    // Solo se agregan los períodos nuevos, reconocidos por id: un período guardado no cambia
    for (PeriodoNecesidad p : domain.getPeriodos()) {
      if (guardados.contains(p.id())) {
        continue;
      }
      PeriodoNecesidadEntity pe = new PeriodoNecesidadEntity();
      pe.setId(p.id());
      pe.setNecesidad(entity);
      pe.setCantidadObjetivo(
          p.cantidadObjetivo() != null ? p.cantidadObjetivo().longValue() : null);
      pe.setFechaFin(p.fechaFin() != null ? p.fechaFin().atStartOfDay() : null);
      pe.setFechaInicio(
          cursorInicio != null
              ? cursorInicio.atStartOfDay()
              : LocalDateTime.now(ZoneId.systemDefault()));

      if (p.fechaFin() != null) {
        cursorInicio = p.fechaFin();
      }
      actuales.add(pe);
    }
  }

  /** El próximo período arranca donde terminó el último guardado, o al inicio de la necesidad. */
  private static LocalDate inicioDelProximoPeriodo(
      NecesidadRecurrente domain, List<PeriodoNecesidadEntity> actuales) {
    if (actuales.isEmpty()) {
      return domain.getFechaInicio();
    }
    LocalDateTime finDelUltimo = actuales.get(actuales.size() - 1).getFechaFin();
    return finDelUltimo != null ? finDelUltimo.toLocalDate() : domain.getFechaInicio();
  }
}
