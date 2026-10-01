package grupo5.incentivos.infrastructure.persistencia.mappers;

import grupo5.incentivos.infrastructure.persistencia.entities.CambioCategoriaEmbeddable;
import grupo5.incentivos.infrastructure.persistencia.entities.DonanteHistorialDonacionEntity;
import grupo5.incentivos.infrastructure.persistencia.entities.DonanteIncentivosEntity;
import grupo5.incentivos.infrastructure.persistencia.entities.InsigniaGanadaEmbeddable;
import grupo5.incentivos.infrastructure.persistencia.entities.MisionEntity;
import grupo5.incentivos.models.entities.donante.CambioCategoria;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import grupo5.incentivos.models.entities.insignias.InsigniaGanada;
import grupo5.incentivos.models.entities.metricas.Metricas;
import grupo5.incentivos.models.entities.misiones.Mision;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DonanteIncentivosPersistenciaMapper {

  private final MisionPersistenciaMapper misionMapper;

  public DonanteIncentivosPersistenciaMapper(MisionPersistenciaMapper misionMapper) {
    this.misionMapper = misionMapper;
  }

  public DonanteIncentivosEntity toEntity(DonanteIncentivos donante) {
    if (donante == null) {
      return null;
    }
    DonanteIncentivosEntity entity = new DonanteIncentivosEntity();
    entity.setId(donante.getId());
    entity.setPersonaId(donante.getIdPersona());
    entity.setNombre(donante.getNombre());
    entity.setCategoria(donante.getCategoria());
    entity.setFechaRegistro(donante.getFechaRegistro());

    donante
        .getHistorialCategorias()
        .forEach(
            c ->
                entity
                    .getHistorialCategorias()
                    .add(
                        new CambioCategoriaEmbeddable(
                            c.getAnterior(), c.getNueva(), c.getFecha())));

    donante
        .getInsignias()
        .forEach(
            i ->
                entity
                    .getInsignias()
                    .add(
                        new InsigniaGanadaEmbeddable(
                            i.nombre(),
                            i.descripcion(),
                            i.imagenUrl(),
                            i.visible(),
                            i.fechaObtenida())));

    // Mismo id de dominio => Hibernate hace merge (UPDATE) en lugar de DELETE + INSERT.
    donante.getMisiones().forEach(m -> entity.getMisiones().add(misionMapper.toEntity(m)));

    Metricas metricas = donante.getMetricas();
    entity.setTotalDonacionesHistoricas(metricas.getTotalDonacionesHistoricas());
    entity.setTotalDonacionesExitosas(metricas.getTotalDonacionesExitosas());
    entity.setUltimaDonacion(metricas.getUltimaDonacion());
    entity.setOrganizacionesAyudadas(new LinkedHashSet<>(metricas.getOrganizacionesAyudadas()));

    List<EventoDonacion> historial = metricas.getHistorialDonaciones();
    for (int i = 0; i < historial.size(); i++) {
      entity.getHistorialDonaciones().add(toHistorialEntity(donante.getId(), i, historial.get(i)));
    }
    return entity;
  }

  public DonanteIncentivos toDomain(DonanteIncentivosEntity entity) {
    if (entity == null) {
      return null;
    }
    List<CambioCategoria> historialCategorias =
        entity.getHistorialCategorias().stream()
            .map(c -> CambioCategoria.reconstituir(c.getAnterior(), c.getNueva(), c.getFecha()))
            .toList();

    List<InsigniaGanada> insignias =
        entity.getInsignias().stream()
            .map(
                i ->
                    new InsigniaGanada(
                        i.getNombre(),
                        i.getDescripcion(),
                        i.getImagenUrl(),
                        i.isVisible(),
                        i.getFechaObtenida()))
            .toList();

    List<Mision> misiones =
        entity.getMisiones().stream()
            .sorted(
                Comparator.comparing(
                        MisionEntity::getNumeroMision,
                        Comparator.nullsLast(Comparator.<Integer>naturalOrder()))
                    .thenComparing(MisionEntity::getId))
            .map(misionMapper::toDomain)
            .toList();

    List<EventoDonacion> historialDonaciones =
        entity.getHistorialDonaciones().stream()
            .sorted(Comparator.comparingInt(DonanteHistorialDonacionEntity::getOrden))
            .map(this::toEventoDonacion)
            .toList();

    Metricas metricas =
        Metricas.reconstituir(
            entity.getTotalDonacionesHistoricas(),
            entity.getTotalDonacionesExitosas(),
            entity.getUltimaDonacion(),
            historialDonaciones,
            entity.getOrganizacionesAyudadas());

    return new DonanteIncentivos(
        entity.getId(),
        entity.getPersonaId(),
        entity.getNombre(),
        entity.getCategoria(),
        entity.getFechaRegistro(),
        new ArrayList<>(historialCategorias),
        new ArrayList<>(misiones),
        new ArrayList<>(insignias),
        metricas);
  }

  private DonanteHistorialDonacionEntity toHistorialEntity(
      UUID donanteId, int orden, EventoDonacion e) {
    DonanteHistorialDonacionEntity entity = new DonanteHistorialDonacionEntity();
    entity.setId(idHistorial(donanteId, orden));
    entity.setOrden(orden);
    entity.setDonacionId(e.getDonacionId());
    entity.setCantidadBienes(e.getCantidadBienes());
    entity.setFecha(e.getFecha());
    entity.setCategorias(new ArrayList<>(e.getCategorias()));
    return entity;
  }

  private EventoDonacion toEventoDonacion(DonanteHistorialDonacionEntity entity) {
    return new EventoDonacion(
        entity.getDonacionId(),
        entity.getCategorias(),
        entity.getCantidadBienes(),
        entity.getFecha());
  }

  /**
   * EventoDonacion no tiene identidad de dominio y el historial solo crece por el final. Derivar el
   * id del par (donante, posición) lo mantiene estable entre guardados: el merge actualiza las
   * filas existentes en vez de borrarlas y reinsertarlas.
   */
  private UUID idHistorial(UUID donanteId, int orden) {
    return UUID.nameUUIDFromBytes(
        (donanteId + ":historial-donacion:" + orden).getBytes(StandardCharsets.UTF_8));
  }
}
