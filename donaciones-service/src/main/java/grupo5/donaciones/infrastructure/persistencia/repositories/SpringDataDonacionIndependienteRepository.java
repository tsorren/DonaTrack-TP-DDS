package grupo5.donaciones.infrastructure.persistencia.repositories;

import grupo5.donaciones.infrastructure.persistencia.entities.donacionesIndependientes.DonacionIndependienteEntity;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataDonacionIndependienteRepository
    extends JpaRepository<DonacionIndependienteEntity, UUID> {

  List<DonacionIndependienteEntity> findByEstadoActual(TipoEstadoDonacion estadoActual);

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

  // Una consulta para todas las necesidades (en vez de una por necesidad): trae una suma por cada
  // período de cada una y el adapter se queda con la del período actual
  @Query(
      "select d.necesidadId as necesidadId, d.periodoId as periodoId, sum(i.cantidad) as cantidad"
          + " from DonacionIndependienteEntity d join d.items i"
          + " where d.necesidadId in :necesidadIds"
          + " group by d.necesidadId, d.periodoId")
  List<SumaPorDestino> sumarCantidadAsignadaPorDestino(
      @Param("necesidadIds") Collection<UUID> necesidadIds);

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
