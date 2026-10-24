package grupo5.donaciones.infrastructure.persistencia.repositories;

import grupo5.donaciones.infrastructure.persistencia.entities.donaciones.DepositoEntity;
import grupo5.donaciones.infrastructure.persistencia.entities.personas.DireccionEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataDepositoRepository extends JpaRepository<DepositoEntity, UUID> {

  Optional<DepositoEntity> findByNombreAndDireccion(String nombre, DireccionEntity direccion);
}
