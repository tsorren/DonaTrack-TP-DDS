package grupo5.donaciones.infrastructure.persistencia.adapters;

import grupo5.donaciones.infrastructure.persistencia.entities.actividad.ActividadEntity;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataActividadRepository;
import grupo5.donaciones.models.entities.actividad.Actividad;
import grupo5.donaciones.models.repositories.IActividadRepository;
import grupo5.donaciones.models.repositories.listados.CriterioActividad;
import grupo5.donaciones.models.repositories.listados.Tramo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * El registro de actividad en PostgreSQL (R11-26): los filtros, el orden y el tramo en la base, con
 * el total de los mismos filtros. Las fechas se guardan en la hora de Buenos Aires: el día pedido
 * va de su medianoche a la del día siguiente.
 */
@Repository
@Profile("postgres")
public class ActividadRepositoryJpaAdapter implements IActividadRepository {

  private final SpringDataActividadRepository filas;

  @PersistenceContext private EntityManager em;

  public ActividadRepositoryJpaAdapter(SpringDataActividadRepository filas) {
    this.filas = filas;
  }

  @Override
  @Transactional
  public void guardar(Actividad actividad) {
    ActividadEntity fila = new ActividadEntity();
    fila.setId(actividad.id());
    fila.setOcurrioEn(actividad.ocurrioEn());
    fila.setAutor(actividad.autor());
    fila.setAccion(actividad.accion());
    fila.setObjetoId(actividad.objetoId());
    fila.setDetalle(actividad.detalle());
    filas.save(fila);
  }

  @Override
  @Transactional(readOnly = true)
  public Tramo<Actividad> buscar(CriterioActividad criterio) {
    ConsultaListado consulta =
        new ConsultaListado("FROM donaciones.actividad_administracion a")
            .siHay(criterio.autores(), "a.autor IN (:autores)", "autores")
            .siHay(
                criterio.acciones().stream().map(Enum::name).toList(),
                "a.accion IN (:acciones)",
                "acciones")
            .siHay(
                criterio.desde() == null ? null : criterio.desde().atStartOfDay(),
                "a.ocurrio_en >= :desde",
                "desde")
            .siHay(
                criterio.hasta() == null ? null : criterio.hasta().plusDays(1).atStartOfDay(),
                "a.ocurrio_en < :hasta",
                "hasta");
    List<UUID> ids = consulta.ids(em, "a.id", "a.ocurrio_en DESC, a.id", criterio.paginado());
    List<Actividad> pagina =
        ConsultaListado.enOrden(
            ids,
            filas.findAllById(ids).stream()
                .map(ActividadRepositoryJpaAdapter::comoActividad)
                .toList(),
            Actividad::id);
    return new Tramo<>(pagina, consulta.total(em));
  }

  private static Actividad comoActividad(ActividadEntity fila) {
    return new Actividad(
        fila.getId(),
        fila.getOcurrioEn(),
        fila.getAutor(),
        fila.getAccion(),
        fila.getObjetoId(),
        fila.getDetalle());
  }
}
