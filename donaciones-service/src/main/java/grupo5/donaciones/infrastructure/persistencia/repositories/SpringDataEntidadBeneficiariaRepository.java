package grupo5.donaciones.infrastructure.persistencia.repositories;

import grupo5.donaciones.infrastructure.persistencia.entities.beneficiarios.EntidadBeneficiariaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataEntidadBeneficiariaRepository
    extends JpaRepository<EntidadBeneficiariaEntity, UUID> {}
