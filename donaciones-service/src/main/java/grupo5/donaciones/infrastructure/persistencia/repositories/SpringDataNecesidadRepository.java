package grupo5.donaciones.infrastructure.persistencia.repositories;

import grupo5.donaciones.infrastructure.persistencia.entities.necesidades.NecesidadEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataNecesidadRepository extends JpaRepository<NecesidadEntity, UUID> {

  List<NecesidadEntity> findByEntidadBeneficiariaId(UUID entidadBeneficiariaId);

  List<NecesidadEntity> findByActiva(boolean activa);

  boolean existsBySubcategoriaId(UUID subcategoriaId);

  // Los períodos de una recurrente son una colección inversa: agregar uno no cambia ninguna
  // columna de la necesidad y @Version no se incrementaría solo. Con este bloqueo la versión sube
  // en cada guardado, y dos renovaciones concurrentes chocan.
  @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
  @Query("select n from NecesidadEntity n where n.id = :id")
  Optional<NecesidadEntity> findByIdForzandoVersion(@Param("id") UUID id);

  @Query("select n from NecesidadEntity n where n.activa = true and type(n) = :tipo")
  List<NecesidadEntity> findActivasDelTipo(@Param("tipo") Class<? extends NecesidadEntity> tipo);
}
