package grupo5.logistica.infrastructure.persistencia.mongo.repositories; // NOSONAR

import grupo5.logistica.infrastructure.persistencia.mongo.documents.SolicitudTransicionEntregaDocument;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

@SuppressWarnings("java:S120")
public interface SpringDataSolicitudTransicionEntregaMongoRepository
    extends MongoRepository<SolicitudTransicionEntregaDocument, UUID> {}
