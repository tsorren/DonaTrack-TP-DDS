package grupo5.incentivos.infrastructure.persistencia.repositories;

import grupo5.incentivos.infrastructure.persistencia.entities.DonanteIncentivosEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface SpringDataDonanteIncentivosRepository
    extends JpaRepository<DonanteIncentivosEntity, UUID> {

  Optional<DonanteIncentivosEntity> findByPersonaId(UUID personaId);

  // No usa los cascades de JPA: los hijos los borra la base (FK con ON DELETE CASCADE).
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Transactional
  @Query("delete from DonanteIncentivosEntity d where d.id = :id")
  int eliminarPorId(@Param("id") UUID id);
}
