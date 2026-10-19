package grupo5.logistica.infrastructure.persistencia.mongo.repositories; // NOSONAR

import grupo5.logistica.infrastructure.persistencia.mongo.documents.CamionDocument;
import grupo5.logistica.models.entities.camiones.EstadoCamion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

@SuppressWarnings("java:S120")
public interface SpringDataCamionMongoRepository extends MongoRepository<CamionDocument, UUID> {
  List<CamionDocument> findByEstadoCamion(EstadoCamion estadoCamion);

  List<CamionDocument> findByEstadoCamionNot(EstadoCamion estadoCamion);

  Optional<CamionDocument> findByPatenteIgnoreCase(String patente);
}
