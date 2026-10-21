package grupo5.logistica.infrastructure.persistencia.repositories;

import grupo5.logistica.infrastructure.persistencia.entities.RutaEntity;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataRutaRepository extends JpaRepository<RutaEntity, UUID> {
  List<RutaEntity> findByFecha(LocalDate fecha);

  List<RutaEntity> findByIdCamion(UUID idCamion);

  List<RutaEntity> findByIdCamionAndFecha(UUID idCamion, LocalDate fecha);

  Optional<RutaEntity> findFirstByIdCamionAndEstado(UUID idCamion, EstadoRuta estado);

  Optional<RutaEntity> findFirstByIdChoferAndEstado(UUID idChofer, EstadoRuta estado);
}
