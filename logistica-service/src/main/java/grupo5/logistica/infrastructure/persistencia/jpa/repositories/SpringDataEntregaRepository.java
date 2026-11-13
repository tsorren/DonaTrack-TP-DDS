package grupo5.logistica.infrastructure.persistencia.jpa.repositories;

import grupo5.logistica.infrastructure.persistencia.jpa.entities.EntregaEntity;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataEntregaRepository extends JpaRepository<EntregaEntity, UUID> {
  List<EntregaEntity> findByEstado(EstadoEntrega estado);

  String DEL_MAPA =
      " FROM EntregaEntity e WHERE e.estado = :estado AND e.horaArribo >= :desde"
          + " AND e.horaArribo < :hasta AND e.direccion.latitud IS NOT NULL"
          + " AND e.direccion.longitud IS NOT NULL";

  /** Las del mapa público (R11-19): el tope lo pone {@code pagina} (siempre la primera). */
  @Query("SELECT e" + DEL_MAPA + " ORDER BY e.horaArribo DESC, e.idEntrega ASC")
  List<EntregaEntity> buscarParaMapa(
      @Param("estado") EstadoEntrega estado,
      @Param("desde") Instant desde,
      @Param("hasta") Instant hasta,
      Pageable pagina);

  @Query("SELECT COUNT(e)" + DEL_MAPA)
  long contarParaMapa(
      @Param("estado") EstadoEntrega estado,
      @Param("desde") Instant desde,
      @Param("hasta") Instant hasta);

  List<EntregaEntity> findByIdRutaOrderByIdEntregaAsc(UUID idRuta);

  List<EntregaEntity> findByIdRutaIsNull();

  boolean existsByIdDonacionAndEstadoNot(UUID idDonacion, EstadoEntrega estado);

  Optional<EntregaEntity> findFirstByIdDonacionAndEstadoNot(UUID idDonacion, EstadoEntrega estado);

  List<EntregaEntity> findByIdDonacionAndEstado(UUID idDonacion, EstadoEntrega estado);

  List<EntregaEntity> findByIdBeneficiarioOrderByIdEntregaAsc(UUID idBeneficiario);

  List<EntregaEntity> findByDonanteIdOrderByIdEntregaAsc(UUID donanteId);
}
