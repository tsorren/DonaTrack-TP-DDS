package grupo5.logistica.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IEntregasRepository extends CrudRepository<Entrega> {

  List<Entrega> findByEstado(EstadoEntrega estado);

  /**
   * Las del mapa público (RF-02, R11-19): las {@code ENTREGADA} con coordenadas cuya hora de arribo
   * cae en {@code [desde, hasta)}, de la más reciente a la más vieja (a igual hora, por id), como
   * mucho {@code limite}. Se resuelve en la consulta: nunca se leen todas las entregadas.
   */
  List<Entrega> findEntregadasParaMapa(Instant desde, Instant hasta, int limite);

  /** Cuántas cumplen {@link #findEntregadasParaMapa} sin el tope, contadas en la base. */
  long contarEntregadasParaMapa(Instant desde, Instant hasta);

  List<Entrega> findByRutaId(UUID rutaId);

  List<Entrega> findSinRuta();

  /** Si la donación tiene una entrega activa (cualquiera que no esté {@code CANCELADA}). */
  boolean existsByIdDonacion(UUID idDonacion);

  /** La entrega activa de la donación: hay a lo sumo una (las canceladas quedan como historia). */
  Optional<Entrega> findByIdDonacion(UUID idDonacion);

  /** Las entregas canceladas de la donación (HB-19). */
  List<Entrega> findCanceladasByIdDonacion(UUID idDonacion);

  List<Entrega> findByIdBeneficiaria(UUID idBeneficiaria);

  List<Entrega> findByDonanteId(UUID donanteId);

  /**
   * Una página del seguimiento (R11-43): el filtro, el orden ({@link CriterioSeguimiento#ORDEN}) y
   * el tramo se resuelven en la base; el total, con una consulta de conteo.
   */
  TramoDeEntregas buscarSeguimiento(CriterioSeguimiento criterio);

  /** Las entregas de esos ids, en una sola lectura; las que no existen no vuelven. */
  List<Entrega> findByIds(Collection<UUID> ids);
}
