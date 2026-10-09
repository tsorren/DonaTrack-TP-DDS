package grupo5.incentivos.infrastructure.persistencia.mappers;

import grupo5.incentivos.infrastructure.persistencia.entities.InsigniaEmbeddable;
import grupo5.incentivos.infrastructure.persistencia.entities.MisionCompletitudEntity;
import grupo5.incentivos.infrastructure.persistencia.entities.MisionDonacionesExitosasEntity;
import grupo5.incentivos.infrastructure.persistencia.entities.MisionEntity;
import grupo5.incentivos.infrastructure.persistencia.entities.MisionHabilDonadorEntity;
import grupo5.incentivos.infrastructure.persistencia.entities.MisionRachaEntity;
import grupo5.incentivos.models.entities.insignias.Insignia;
import grupo5.incentivos.models.entities.misiones.*;
import grupo5.incentivos.models.storage.IImagenesInsignias;
import java.util.LinkedHashSet;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Mapea cada subtipo de {@link Mision} conservando el id de dominio (evita key churn). */
@Component
public class MisionPersistenciaMapper {

  private final IImagenesInsignias imagenes;

  // ObjectProvider: sin MinIO (en memoria, slices de test) no hay bean y se usa la identidad.
  @Autowired
  public MisionPersistenciaMapper(ObjectProvider<IImagenesInsignias> imagenes) {
    this(imagenes.getIfAvailable(() -> IImagenesInsignias.IDENTIDAD));
  }

  public MisionPersistenciaMapper(IImagenesInsignias imagenes) {
    this.imagenes = imagenes;
  }

  /** Referencia de imagen -> URL pública. También lo usa el mapper de insignias ganadas. */
  public String resolverImagenUrl(String referencia) {
    return imagenes.urlPublica(referencia);
  }

  /** URL pública -> referencia que se guarda en la base. */
  public String referenciaImagen(String url) {
    return imagenes.referencia(url);
  }

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
    MisionEstado estado = toEstado(entity);
    if (entity instanceof MisionCompletitudEntity e) {
      return MisionCompletitud.reconstituir(estado, e.getCategoriasDonadas());
    }
    if (entity instanceof MisionDonacionesExitosasEntity e) {
      return MisionDonacionesExitosas.reconstituir(estado, e.getFechaUltimoDonacion());
    }
    if (entity instanceof MisionHabilDonadorEntity) {
      return MisionHabilDonador.reconstituir(estado);
    }
    if (entity instanceof MisionRachaEntity e) {
      return MisionRacha.reconstituir(estado, e.getUltimoMesDonado());
    }
    throw new IllegalArgumentException(
        "Tipo de misión sin mapeo de persistencia: " + entity.getClass().getName());
  }

  private MisionEstado toEstado(MisionEntity e) {
    return new MisionEstado(
        e.getId(),
        e.getNumeroMision(),
        e.getNombre(),
        e.getDescripcion(),
        e.getCategoria(),
        e.getObjetivo(),
        e.getProgresoActual(),
        e.isCompletada(),
        e.getFechaCompletada(),
        toInsignia(e.getInsignia()));
  }

  private InsigniaEmbeddable toEmbeddable(Insignia insignia) {
    if (insignia == null) {
      return null;
    }
    return new InsigniaEmbeddable(
        insignia.nombre(), insignia.descripcion(), referenciaImagen(insignia.imagenUrl()));
  }

  private Insignia toInsignia(InsigniaEmbeddable embeddable) {
    if (embeddable == null || embeddable.getNombre() == null) {
      return null;
    }
    return new Insignia(
        embeddable.getNombre(),
        embeddable.getDescripcion(),
        resolverImagenUrl(embeddable.getImagenUrl()));
  }
}
