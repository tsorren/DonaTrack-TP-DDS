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

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Transactional
  @Query("update DonanteIncentivosEntity d set d.nombre = :nombre where d.id = :id")
  int actualizarNombre(@Param("id") UUID id, @Param("nombre") String nombre);

  // JPQL no permite UPDATE sobre elementos de un @ElementCollection: va SQL nativo.
  // {h-schema} resuelve hibernate.default_schema (incentivos) en consultas nativas.
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Transactional
  @Query(
      value =
          "UPDATE {h-schema}donante_insignia_ganada SET visible = :visible "
              + "WHERE donante_id = :donanteId AND nombre = :nombre",
      nativeQuery = true)
  int actualizarVisibilidadInsignia(
      @Param("donanteId") UUID donanteId,
      @Param("nombre") String nombre,
      @Param("visible") boolean visible);

  // No usa los cascades de JPA: los hijos los borra la base (FK con ON DELETE CASCADE).
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Transactional
  @Query("delete from DonanteIncentivosEntity d where d.id = :id")
  int eliminarPorId(@Param("id") UUID id);
}
