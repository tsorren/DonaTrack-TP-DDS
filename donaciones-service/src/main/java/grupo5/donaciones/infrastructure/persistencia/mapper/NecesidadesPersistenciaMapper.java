package grupo5.donaciones.infrastructure.persistencia.mapper;

import grupo5.donaciones.infrastructure.persistencia.entities.necesidades.NecesidadEntity;
import grupo5.donaciones.infrastructure.persistencia.entities.necesidades.NecesidadExtraordinariaEntity;
import grupo5.donaciones.infrastructure.persistencia.entities.necesidades.NecesidadRecurrenteEntity;
import grupo5.donaciones.infrastructure.persistencia.entities.necesidades.PeriodoNecesidadEntity;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.entities.necesidades.PeriodoNecesidad;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
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

      if (rec.getPeriodo() != null) {
        int totalDias = rec.getPeriodo().getDays() + (rec.getPeriodo().getMonths() * 30);
        entity.setPeriodo(totalDias > 0 ? totalDias : 30);
      } else {
        entity.setPeriodo(null);
      }

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

    LocalDate fechaInicio =
        entity.getFechaInicio() != null ? entity.getFechaInicio().toLocalDate() : LocalDate.now();
    Integer cantidad =
        entity.getCantidadNecesitada() != null ? entity.getCantidadNecesitada().intValue() : 0;

    if (entity instanceof NecesidadExtraordinariaEntity extraEntity) {
      return new NecesidadExtraordinaria(
          extraEntity.getId(),
          extraEntity.getSubcategoriaId(),
          cantidad,
          extraEntity.getDescripcion(),
          fechaInicio,
          extraEntity.getEntidadBeneficiariaId(),
          extraEntity.isActiva(),
          List.of());
    }

    if (entity instanceof NecesidadRecurrenteEntity recEntity) {
      Period periodo =
          recEntity.getPeriodo() != null
              ? Period.ofDays(recEntity.getPeriodo())
              : Period.ofMonths(1);

      List<PeriodoNecesidad> periodosDominio = new ArrayList<>();
      if (recEntity.getPeriodos() != null) {
        for (PeriodoNecesidadEntity pe : recEntity.getPeriodos()) {
          LocalDate fechaFin = pe.getFechaFin() != null ? pe.getFechaFin().toLocalDate() : null;
          Integer cantidadObjetivo =
              pe.getCantidadObjetivo() != null ? pe.getCantidadObjetivo().intValue() : 0;
          periodosDominio.add(new PeriodoNecesidad(fechaFin, List.of(), cantidadObjetivo, null));
        }
      }

      NecesidadRecurrente recurrente =
          new NecesidadRecurrente(
              recEntity.getId(),
              recEntity.getSubcategoriaId(),
              cantidad,
              recEntity.getDescripcion(),
              fechaInicio,
              recEntity.getEntidadBeneficiariaId(),
              periodo,
              recEntity.isActiva(),
              periodosDominio);

      // Vincular el Aggregate Root padre en cada período
      List<PeriodoNecesidad> periodosVinculados =
          periodosDominio.stream().map(p -> p.conNecesidadRecurrente(recurrente)).toList();
      recurrente.actualizarPeriodos(periodosVinculados);

      return recurrente;
    }

    throw new IllegalArgumentException(
        "Tipo de entidad no soportado: " + entity.getClass().getName());
  }

  private void mapearCamposComunes(Necesidad domain, NecesidadEntity entity) {
    entity.setId(domain.getId());
    entity.setSubcategoriaId(domain.getSubcategoriaId());
    entity.setEntidadBeneficiariaId(domain.getEntidadId());
    entity.setCantidadNecesitada(
        domain.getCantidadNecesitada() != null ? domain.getCantidadNecesitada().longValue() : null);
    entity.setDescripcion(domain.getDescripcion());
    entity.setFechaInicio(
        domain.getFechaInicio() != null
            ? domain.getFechaInicio().atStartOfDay()
            : LocalDateTime.now());
    entity.setActiva(domain.isActiva());
  }

  private void sincronizarPeriodos(NecesidadRecurrente domain, NecesidadRecurrenteEntity entity) {
    List<PeriodoNecesidadEntity> actuales = entity.getPeriodos();
    if (actuales == null) {
      actuales = new ArrayList<>();
      entity.setPeriodos(actuales);
    }

    LocalDate cursorInicio =
        actuales.isEmpty()
            ? domain.getFechaInicio()
            : (actuales.get(actuales.size() - 1).getFechaFin() != null
                ? actuales.get(actuales.size() - 1).getFechaFin().toLocalDate()
                : domain.getFechaInicio());

    // Sincronización incremental sin invocar clear()
    for (int i = actuales.size(); i < domain.getPeriodos().size(); i++) {
      PeriodoNecesidad p = domain.getPeriodos().get(i);
      PeriodoNecesidadEntity pe = new PeriodoNecesidadEntity();
      pe.setNecesidad(entity);
      pe.setCantidadObjetivo(
          p.cantidadObjetivo() != null ? p.cantidadObjetivo().longValue() : null);
      pe.setFechaFin(p.fechaFin() != null ? p.fechaFin().atStartOfDay() : null);
      pe.setFechaInicio(cursorInicio != null ? cursorInicio.atStartOfDay() : LocalDateTime.now());

      if (p.fechaFin() != null) {
        cursorInicio = p.fechaFin();
      }
      actuales.add(pe);
    }
  }
}
