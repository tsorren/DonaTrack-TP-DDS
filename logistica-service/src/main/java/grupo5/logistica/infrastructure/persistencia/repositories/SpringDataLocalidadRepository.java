package grupo5.logistica.infrastructure.persistencia.repositories;

import grupo5.logistica.infrastructure.persistencia.entities.LocalidadEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataLocalidadRepository extends JpaRepository<LocalidadEntity, UUID> {
  Optional<LocalidadEntity> findByProvincia_IdProvinciaAndNombreIgnoreCase(
      UUID idProvincia, String nombre);
}
