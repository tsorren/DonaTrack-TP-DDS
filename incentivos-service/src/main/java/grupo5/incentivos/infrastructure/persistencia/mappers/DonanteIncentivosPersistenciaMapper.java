package grupo5.incentivos.infrastructure.persistencia.mappers;

import grupo5.incentivos.infrastructure.persistencia.entities.CambioCategoriaEmbeddable;
import grupo5.incentivos.infrastructure.persistencia.entities.DonanteIncentivosEntity;
import grupo5.incentivos.infrastructure.persistencia.entities.InsigniaGanadaEmbeddable;
import grupo5.incentivos.infrastructure.persistencia.entities.MisionEntity;
import grupo5.incentivos.models.entities.donante.CambioCategoria;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.insignias.InsigniaGanada;
import grupo5.incentivos.models.entities.metricas.Metricas;
import grupo5.incentivos.models.entities.misiones.Mision;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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

    metricas
        .donacionesPorPeriodo()
        .forEach(
            (periodo, cantidad) ->
                entity.getDonacionesPorPeriodo().put(periodo.toString(), cantidad));
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

    Map<YearMonth, Long> donacionesPorPeriodo = new TreeMap<>();
    entity
        .getDonacionesPorPeriodo()
        .forEach(
            (periodo, cantidad) -> donacionesPorPeriodo.put(YearMonth.parse(periodo), cantidad));

    Metricas metricas =
        Metricas.reconstituir(
            entity.getTotalDonacionesHistoricas(),
            entity.getTotalDonacionesExitosas(),
            entity.getUltimaDonacion(),
            donacionesPorPeriodo,
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
}
