package grupo5.donaciones.infrastructure.persistencia.repositories;

import grupo5.donaciones.infrastructure.persistencia.entities.propuestas.PropuestaEntity;
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataPropuestaRepository extends JpaRepository<PropuestaEntity, UUID> {

  long countByEstado(EstadoPropuesta estado);
}
