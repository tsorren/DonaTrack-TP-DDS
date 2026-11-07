package grupo5.donaciones.infrastructure.persistencia.repositories;

import grupo5.donaciones.infrastructure.persistencia.entities.itemsNormalizados.ItemDonacionNormalizadoEntity;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataItemDonacionNormalizadoRepository
    extends JpaRepository<ItemDonacionNormalizadoEntity, UUID> {

  boolean existsBySubcategoriaId(UUID subcategoriaId);

  List<ItemDonacionNormalizadoEntity> findByDonacionId(UUID donacionId);

  List<ItemDonacionNormalizadoEntity> findByEstadoNormalizacion(
      EstadoNormalizacion estadoNormalizacion);
}
