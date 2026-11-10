package grupo5.donaciones.infrastructure.persistencia.repositories;

import grupo5.donaciones.infrastructure.persistencia.entities.donantes.ArchivoFilaErrorEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataArchivoFilaErrorRepository
    extends JpaRepository<ArchivoFilaErrorEntity, Long> {

  List<ArchivoFilaErrorEntity> findByArchivoIdOrderByLineaAsc(UUID archivoId, Pageable pagina);
}
