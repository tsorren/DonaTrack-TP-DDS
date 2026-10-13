package grupo5.donaciones.infrastructure.persistencia.repositories;

import grupo5.donaciones.infrastructure.persistencia.entities.necesidades.NecesidadEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataNecesidadRepository extends JpaRepository<NecesidadEntity, UUID> {

  List<NecesidadEntity> findByEntidadBeneficiariaId(UUID entidadBeneficiariaId);

  List<NecesidadEntity> findByActiva(boolean activa);
}
