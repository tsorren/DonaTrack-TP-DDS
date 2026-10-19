package grupo5.logistica.infrastructure.persistencia.mongo.repositories; // NOSONAR

import grupo5.logistica.infrastructure.persistencia.mongo.documents.ChoferDocument;
import grupo5.logistica.models.entities.choferes.EstadoChofer;
import java.util.List;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

@SuppressWarnings("java:S120")
public interface SpringDataChoferMongoRepository extends MongoRepository<ChoferDocument, UUID> {
  List<ChoferDocument> findByEstadoChofer(EstadoChofer estadoChofer);

  List<ChoferDocument> findByEstadoChoferNot(EstadoChofer estadoChofer);
}
