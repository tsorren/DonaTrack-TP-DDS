package grupo5.logistica.infrastructure.persistencia.mongo.repositories; // NOSONAR

import grupo5.logistica.infrastructure.persistencia.mongo.documents.RutaDocument;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

@SuppressWarnings("java:S120")
public interface SpringDataRutaMongoRepository extends MongoRepository<RutaDocument, UUID> {
  List<RutaDocument> findByFecha(LocalDate fecha);

  List<RutaDocument> findByIdCamion(UUID idCamion);

  List<RutaDocument> findByIdCamionAndFecha(UUID idCamion, LocalDate fecha);

  Optional<RutaDocument> findFirstByIdCamionAndEstado(UUID idCamion, EstadoRuta estado);

  Optional<RutaDocument> findFirstByIdChoferAndEstado(UUID idChofer, EstadoRuta estado);
}
