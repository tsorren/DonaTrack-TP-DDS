package grupo5.logistica.infrastructure.persistencia.mongo.repositories; // NOSONAR

import grupo5.logistica.infrastructure.persistencia.mongo.documents.EntregaDocument;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

@SuppressWarnings("java:S120")
public interface SpringDataEntregaMongoRepository extends MongoRepository<EntregaDocument, UUID> {
  List<EntregaDocument> findByEstado(EstadoEntrega estado);

  String DEL_MAPA =
      "{ 'estado': ?0, 'horaArribo': { '$gte': ?1, '$lt': ?2 },"
          + " 'destino.ubicacion': { '$ne': null } }";

  /** Las del mapa público (R11-19): el orden y el tope los pone {@code pagina}. */
  @Query(DEL_MAPA)
  List<EntregaDocument> buscarParaMapa(
      EstadoEntrega estado, Instant desde, Instant hasta, Pageable pagina);

  @Query(value = DEL_MAPA, count = true)
  long contarParaMapa(EstadoEntrega estado, Instant desde, Instant hasta);

  List<EntregaDocument> findByAsignacionIdRutaOrderByIdAsc(UUID idRuta);

  List<EntregaDocument> findByAsignacionIsNull();

  boolean existsByIdDonacionAndEstadoNot(UUID idDonacion, EstadoEntrega estado);

  Optional<EntregaDocument> findFirstByIdDonacionAndEstadoNot(
      UUID idDonacion, EstadoEntrega estado);

  List<EntregaDocument> findByIdDonacionAndEstado(UUID idDonacion, EstadoEntrega estado);

  List<EntregaDocument> findByIdBeneficiaria(UUID idBeneficiaria);

  List<EntregaDocument> findByDonanteId(UUID donanteId);
}
