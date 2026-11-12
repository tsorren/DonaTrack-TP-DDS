package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.repositories.listados.ConteoAsignadas;
import grupo5.donaciones.models.repositories.listados.CriterioDonacionesIndependientes;
import grupo5.donaciones.models.repositories.listados.LecturaPorLote;
import grupo5.donaciones.models.repositories.listados.PorVencer;
import grupo5.donaciones.models.repositories.listados.Tramo;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface IDonacionesIndependientesRepository
    extends CrudRepository<DonacionIndependiente>, LecturaPorLote<DonacionIndependiente> {

  /** Una página con la búsqueda, los filtros y el orden resueltos (R11-01). */
  Tramo<DonacionIndependiente> buscar(CriterioDonacionesIndependientes criterio);

  /** Cuántas hay en cada estado; los estados sin donaciones no aparecen. */
  Map<TipoEstadoDonacion, Long> contarPorEstado();

  /**
   * Cuántas donaciones pasaron a {@code estado} con un cambio en {@code [desde, hasta)} (entregadas
   * en el mes del resumen del admin).
   */
  long contarQuePasaronA(TipoEstadoDonacion estado, LocalDateTime desde, LocalDateTime hasta);

  /**
   * Lo mismo que {@link #contarQuePasaronA(TipoEstadoDonacion, LocalDateTime, LocalDateTime)} sobre
   * las asignadas a una organización y, si no es nula, a una necesidad (R11-43).
   */
  long contarQuePasaronA(
      TipoEstadoDonacion estado,
      LocalDateTime desde,
      LocalDateTime hasta,
      UUID entidadId,
      UUID necesidadId);

  /**
   * Cuántas asignadas a la organización hay por necesidad y estado (R11-43), con una sola consulta.
   * Con {@code necesidadId}, solo las de esa necesidad. Las combinaciones sin donaciones no
   * aparecen.
   */
  List<ConteoAsignadas> contarAsignadas(UUID entidadId, UUID necesidadId);

  /**
   * Las que siguen en depósito con algún bien que vence entre {@code desde} y {@code hasta}
   * (inclusivos): cuántas son y cuál vence primero.
   */
  PorVencer buscarPorVencer(LocalDate desde, LocalDate hasta);

  List<DonacionIndependiente> findEnDeposito();

  /**
   * Las donaciones asignadas a la necesidad que todavía no salieron ({@code ASIGNACION_REALIZADA} o
   * {@code LISTA_PARA_ENTREGAR}): las que se cancelan al darla de baja (HB-19).
   */
  List<DonacionIndependiente> findCancelablesPorNecesidad(UUID necesidadId);

  /**
   * Cantidad asignada a una necesidad: la suma de las donaciones con ese destino. {@code periodoId}
   * es el período en las recurrentes y {@code null} en las extraordinarias (DTI-06).
   */
  int sumarCantidadAsignada(UUID necesidadId, UUID periodoId);

  /**
   * Lo mismo que {@link #sumarCantidadAsignada(UUID, UUID)} para varias necesidades en una sola
   * consulta. La clave es el id de la necesidad y el valor su período (nulo en las
   * extraordinarias). Devuelve una entrada por cada necesidad, con 0 si no tiene nada asignado.
   */
  Map<UUID, Integer> sumarCantidadAsignada(Map<UUID, UUID> periodoPorNecesidad);

  /**
   * Lo recibido por una necesidad: como {@link #sumarCantidadAsignada(UUID, UUID)}, pero solo las
   * donaciones {@code ENTREGADA} (ADR 20261102 del ciclo de vida de la necesidad, punto 6).
   */
  int sumarCantidadRecibida(UUID necesidadId, UUID periodoId);

  /** Lo mismo que {@link #sumarCantidadRecibida(UUID, UUID)} para varias necesidades. */
  Map<UUID, Integer> sumarCantidadRecibida(Map<UUID, UUID> periodoPorNecesidad);

  /** Cuántas donaciones asignadas a cada entidad se registraron después de {@code desde}. */
  Map<UUID, Integer> contarPorEntidadRegistradasDesde(LocalDateTime desde);

  /** Cuántas donaciones están hoy en {@code estado} (contadores públicos, HB-02). */
  long contarEnEstado(TipoEstadoDonacion estado);

  /**
   * Destacadas (HB-01, D28): entregadas desde {@code desde} con fotos replicadas, de la más nueva a
   * la más vieja, hasta {@code limite}.
   */
  List<DonacionIndependiente> findDestacadasDesde(LocalDateTime desde, int limite);

  /** Cuántas destacadas hay desde {@code desde}, sin el límite. */
  long contarDestacadasDesde(LocalDateTime desde);
}
