package grupo5.incentivos.infrastructure.persistencia.mappers;

import grupo5.incentivos.infrastructure.persistencia.entities.InsigniaEmbeddable;
import grupo5.incentivos.infrastructure.persistencia.entities.MisionCompletitudEntity;
import grupo5.incentivos.infrastructure.persistencia.entities.MisionDonacionesExitosasEntity;
import grupo5.incentivos.infrastructure.persistencia.entities.MisionEntity;
import grupo5.incentivos.infrastructure.persistencia.entities.MisionHabilDonadorEntity;
import grupo5.incentivos.infrastructure.persistencia.entities.MisionRachaEntity;
import grupo5.incentivos.models.entities.insignias.Insignia;
import grupo5.incentivos.models.entities.misiones.Mision;
import grupo5.incentivos.models.entities.misiones.MisionCompletitud;
import grupo5.incentivos.models.entities.misiones.MisionDonacionesExitosas;
import grupo5.incentivos.models.entities.misiones.MisionHabilDonador;
import grupo5.incentivos.models.entities.misiones.MisionRacha;
import java.util.LinkedHashSet;
import org.springframework.stereotype.Component;

/** Mapea cada subtipo de {@link Mision} conservando el id de dominio (evita key churn). */
@Component
public class MisionPersistenciaMapper {

  public MisionEntity toEntity(Mision mision) {
    if (mision == null) {
      return null;
    }
    MisionEntity entity;
    if (mision instanceof MisionCompletitud completitud) {
      MisionCompletitudEntity e = new MisionCompletitudEntity();
      e.setCategoriasDonadas(new LinkedHashSet<>(completitud.getCategoriasDonadas()));
      entity = e;
    } else if (mision instanceof MisionDonacionesExitosas exitosas) {
      MisionDonacionesExitosasEntity e = new MisionDonacionesExitosasEntity();
      e.setFechaUltimoDonacion(exitosas.getFechaUltimoDonacion());
      entity = e;
    } else if (mision instanceof MisionHabilDonador) {
      entity = new MisionHabilDonadorEntity();
    } else if (mision instanceof MisionRacha racha) {
      MisionRachaEntity e = new MisionRachaEntity();
      e.setUltimoMesDonado(racha.getUltimoMesDonado());
      entity = e;
    } else {
      throw new IllegalArgumentException(
          "Tipo de misión sin mapeo de persistencia: " + mision.getClass().getName());
    }
    entity.setId(mision.getId());
    entity.setNumeroMision(mision.getNumeroMision());
    entity.setNombre(mision.getNombre());
    entity.setDescripcion(mision.getDescripcion());
    entity.setCategoria(mision.getCategoria());
    entity.setObjetivo(mision.getObjetivo());
    entity.setProgresoActual(mision.getProgresoActual());
    entity.setCompletada(mision.isCompletada());
    entity.setFechaCompletada(mision.getFechaCompletada());
    entity.setInsignia(toEmbeddable(mision.getInsignia()));
    return entity;
  }

  public Mision toDomain(MisionEntity entity) {
    if (entity == null) {
      return null;
    }
    Insignia insignia = toInsignia(entity.getInsignia());
    if (entity instanceof MisionCompletitudEntity e) {
      return MisionCompletitud.reconstituir(
          e.getId(),
          e.getNumeroMision(),
          e.getNombre(),
          e.getDescripcion(),
          e.getCategoria(),
          e.getObjetivo(),
          e.getProgresoActual(),
          e.isCompletada(),
          e.getFechaCompletada(),
          insignia,
          e.getCategoriasDonadas());
    }
    if (entity instanceof MisionDonacionesExitosasEntity e) {
      return MisionDonacionesExitosas.reconstituir(
          e.getId(),
          e.getNumeroMision(),
          e.getNombre(),
          e.getDescripcion(),
          e.getCategoria(),
          e.getObjetivo(),
          e.getProgresoActual(),
          e.isCompletada(),
          e.getFechaCompletada(),
          insignia,
          e.getFechaUltimoDonacion());
    }
    if (entity instanceof MisionHabilDonadorEntity e) {
      return MisionHabilDonador.reconstituir(
          e.getId(),
          e.getNumeroMision(),
          e.getNombre(),
          e.getDescripcion(),
          e.getCategoria(),
          e.getObjetivo(),
          e.getProgresoActual(),
          e.isCompletada(),
          e.getFechaCompletada(),
          insignia);
    }
    if (entity instanceof MisionRachaEntity e) {
      return MisionRacha.reconstituir(
          e.getId(),
          e.getNumeroMision(),
          e.getNombre(),
          e.getDescripcion(),
          e.getCategoria(),
          e.getObjetivo(),
          e.getProgresoActual(),
          e.isCompletada(),
          e.getFechaCompletada(),
          insignia,
          e.getUltimoMesDonado());
    }
    throw new IllegalArgumentException(
        "Tipo de misión sin mapeo de persistencia: " + entity.getClass().getName());
  }

  private InsigniaEmbeddable toEmbeddable(Insignia insignia) {
    if (insignia == null) {
      return null;
    }
    return new InsigniaEmbeddable(insignia.nombre(), insignia.descripcion(), insignia.imagenUrl());
  }

  private Insignia toInsignia(InsigniaEmbeddable embeddable) {
    if (embeddable == null || embeddable.getNombre() == null) {
      return null;
    }
    return new Insignia(
        embeddable.getNombre(), embeddable.getDescripcion(), embeddable.getImagenUrl());
  }
}
