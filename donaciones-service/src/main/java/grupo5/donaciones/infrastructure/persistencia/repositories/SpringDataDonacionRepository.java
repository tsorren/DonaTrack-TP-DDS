package grupo5.donaciones.infrastructure.persistencia.repositories;

import grupo5.donaciones.infrastructure.persistencia.entities.donaciones.DonacionEntity;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataDonacionRepository extends JpaRepository<DonacionEntity, UUID> {

  @Query("select distinct d.donanteId from DonacionEntity d")
  Set<UUID> findIdsDeDonantes();
}
