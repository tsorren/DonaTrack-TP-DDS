package grupo5.logistica.infrastructure.persistencia.mongo.repositories; // NOSONAR

import grupo5.logistica.infrastructure.persistencia.mongo.documents.EntregaDocument;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

@SuppressWarnings("java:S120")
public interface SpringDataEntregaMongoRepository extends MongoRepository<EntregaDocument, UUID> {
  List<EntregaDocument> findByEstado(EstadoEntrega estado);

  List<EntregaDocument> findByAsignacionIdRutaOrderByIdAsc(UUID idRuta);

  List<EntregaDocument> findByAsignacionIsNull();

  boolean existsByIdDonacion(UUID idDonacion);

  Optional<EntregaDocument> findByIdDonacion(UUID idDonacion);

  List<EntregaDocument> findByIdBeneficiaria(UUID idBeneficiaria);
}
