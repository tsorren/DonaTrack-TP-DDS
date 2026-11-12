package grupo5.donaciones.infrastructure.persistencia.repositories;

import grupo5.donaciones.infrastructure.persistencia.entities.donacionesIndependientes.DonacionIndependienteEntity;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataDonacionIndependienteRepository
    extends JpaRepository<DonacionIndependienteEntity, UUID> {

  List<DonacionIndependienteEntity> findByEstadoActual(TipoEstadoDonacion estadoActual);

  long countByEstadoActual(TipoEstadoDonacion estadoActual);

  @Query(
      "select d.estadoActual as estado, count(d) as cantidad"
          + " from DonacionIndependienteEntity d group by d.estadoActual")
  List<ConteoPorEstado> contarPorEstado();

  interface ConteoPorEstado {
    TipoEstadoDonacion getEstado();

    long getCantidad();
  }

  // HB-01: fecha_entrega solo se guarda junto con fotos publicables (V15)
  List<DonacionIndependienteEntity>
      findByEstadoActualAndFechaEntregaGreaterThanEqualOrderByFechaEntregaDescIdAsc(
          TipoEstadoDonacion estadoActual, LocalDateTime desde, Limit limite);

  long countByEstadoActualAndFechaEntregaGreaterThanEqual(
      TipoEstadoDonacion estadoActual, LocalDateTime desde);

  List<DonacionIndependienteEntity> findByNecesidadIdAndEstadoActualIn(
      UUID necesidadId, Collection<TipoEstadoDonacion> estados);

  // Dos consultas en vez de una con «:periodoId is null»: Postgres no infiere el tipo de un
  // parámetro nulo
  @Query(
      "select coalesce(sum(i.cantidad), 0) from DonacionIndependienteEntity d join d.items i"
          + " where d.necesidadId = :necesidadId and d.periodoId = :periodoId")
  long sumarCantidadAsignadaEnPeriodo(
      @Param("necesidadId") UUID necesidadId, @Param("periodoId") UUID periodoId);

  @Query(
      "select coalesce(sum(i.cantidad), 0) from DonacionIndependienteEntity d join d.items i"
          + " where d.necesidadId = :necesidadId and d.periodoId is null")
  long sumarCantidadAsignadaSinPeriodo(@Param("necesidadId") UUID necesidadId);

  // Lo que no llegó (HB-17) va aparte: sumado junto a los ítems se contaría una vez por ítem
  @Query(
      "select coalesce(sum(d.cantidadNoRecibida), 0) from DonacionIndependienteEntity d"
          + " where d.necesidadId = :necesidadId and d.periodoId = :periodoId")
  long sumarNoRecibidoEnPeriodo(
      @Param("necesidadId") UUID necesidadId, @Param("periodoId") UUID periodoId);

  @Query(
      "select coalesce(sum(d.cantidadNoRecibida), 0) from DonacionIndependienteEntity d"
          + " where d.necesidadId = :necesidadId and d.periodoId is null")
  long sumarNoRecibidoSinPeriodo(@Param("necesidadId") UUID necesidadId);

  @Query(
      "select d.necesidadId as necesidadId, d.periodoId as periodoId,"
          + " sum(d.cantidadNoRecibida) as cantidad"
          + " from DonacionIndependienteEntity d"
          + " where d.necesidadId in :necesidadIds and d.cantidadNoRecibida is not null"
          + " group by d.necesidadId, d.periodoId")
  List<SumaPorDestino> sumarNoRecibidoPorDestino(
      @Param("necesidadIds") Collection<UUID> necesidadIds);

  @Query(
      "select d.entidadBeneficiariaId as entidadId, count(d) as cantidad"
          + " from DonacionIndependienteEntity d"
          + " where d.entidadBeneficiariaId is not null and d.fechaRegistro > :desde"
          + " group by d.entidadBeneficiariaId")
  List<ConteoPorEntidad> contarPorEntidadRegistradasDesde(@Param("desde") LocalDateTime desde);

  @Query(
      "select d.entidadBeneficiariaId as entidadId, count(d) as cantidad"
          + " from DonacionIndependienteEntity d"
          + " where d.entidadBeneficiariaId is not null"
          + " group by d.entidadBeneficiariaId")
  List<ConteoPorEntidad> contarPorEntidad();

  // Compare-and-swap de la versión: la fila queda tomada por quien la sube. Si otra operación
  // guardó antes, no matchea ninguna fila y devuelve 0. Hibernate no sube solo la versión cuando
  // solo cambian los ítems (colección inversa), así que se hace explícito; clearAutomatically
  // evita leer después una copia vieja del contexto de persistencia.
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      "update DonacionIndependienteEntity d set d.version = d.version + 1"
          + " where d.id = :id and d.version = :version")
  int subirVersion(@Param("id") UUID id, @Param("version") Long version);

  // Una consulta para todas las necesidades (en vez de una por necesidad): trae una suma por cada
  // período de cada una y el adapter se queda con la del período actual
  @Query(
      "select d.necesidadId as necesidadId, d.periodoId as periodoId, sum(i.cantidad) as cantidad"
          + " from DonacionIndependienteEntity d join d.items i"
          + " where d.necesidadId in :necesidadIds"
          + " group by d.necesidadId, d.periodoId")
  List<SumaPorDestino> sumarCantidadAsignadaPorDestino(
      @Param("necesidadIds") Collection<UUID> necesidadIds);

  // Lo recibido: lo mismo que lo asignado, solo con las donaciones en el estado indicado
  // (ENTREGADA)
  @Query(
      "select coalesce(sum(i.cantidad), 0) from DonacionIndependienteEntity d join d.items i"
          + " where d.necesidadId = :necesidadId and d.periodoId = :periodoId"
          + " and d.estadoActual = :estado")
  long sumarCantidadEnPeriodoConEstado(
      @Param("necesidadId") UUID necesidadId,
      @Param("periodoId") UUID periodoId,
      @Param("estado") TipoEstadoDonacion estado);

  @Query(
      "select coalesce(sum(i.cantidad), 0) from DonacionIndependienteEntity d join d.items i"
          + " where d.necesidadId = :necesidadId and d.periodoId is null"
          + " and d.estadoActual = :estado")
  long sumarCantidadSinPeriodoConEstado(
      @Param("necesidadId") UUID necesidadId, @Param("estado") TipoEstadoDonacion estado);

  @Query(
      "select d.necesidadId as necesidadId, d.periodoId as periodoId, sum(i.cantidad) as cantidad"
          + " from DonacionIndependienteEntity d join d.items i"
          + " where d.necesidadId in :necesidadIds and d.estadoActual = :estado"
          + " group by d.necesidadId, d.periodoId")
  List<SumaPorDestino> sumarCantidadPorDestinoConEstado(
      @Param("necesidadIds") Collection<UUID> necesidadIds,
      @Param("estado") TipoEstadoDonacion estado);

  interface SumaPorDestino {
    UUID getNecesidadId();

    UUID getPeriodoId();

    long getCantidad();
  }

  interface ConteoPorEntidad {
    UUID getEntidadId();

    long getCantidad();
  }
}
