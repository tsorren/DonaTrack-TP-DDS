package grupo5.logistica.infrastructure.persistencia.mongo.adapters; // NOSONAR

import grupo5.logistica.infrastructure.persistencia.jpa.entities.TipoTransicionEntrega;
import grupo5.logistica.infrastructure.persistencia.mongo.documents.SolicitudTransicionEntregaDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.repositories.SpringDataSolicitudTransicionEntregaMongoRepository;
import grupo5.logistica.models.entities.entregas.ConfirmacionRecepcion;
import grupo5.logistica.models.entities.entregas.NoRecepcion;
import grupo5.logistica.models.entities.entregas.RegresoDeposito;
import grupo5.logistica.models.entities.entregas.RevisionEntrega;
import grupo5.logistica.models.entities.entregas.SolicitudTransicionEntrega;
import grupo5.logistica.models.repositories.ISolicitudesTransicionEntregaRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@SuppressWarnings("java:S120")
@Repository
@Profile("mongo")
public class SolicitudesTransicionEntregaRepositoryMongoAdapter
    implements ISolicitudesTransicionEntregaRepository {

  private final SpringDataSolicitudTransicionEntregaMongoRepository springDataRepo;

  public SolicitudesTransicionEntregaRepositoryMongoAdapter(
      SpringDataSolicitudTransicionEntregaMongoRepository springDataRepo) {
    this.springDataRepo = springDataRepo;
  }

  @Override
  public void registrar(SolicitudTransicionEntrega solicitud) {
    if (solicitud == null || solicitud.entrega() == null) {
      return;
    }

    SolicitudTransicionEntregaDocument doc = new SolicitudTransicionEntregaDocument();
    doc.setId(UUID.randomUUID());
    doc.setIdEntrega(solicitud.entrega().getId());
    doc.setActor(solicitud.actor());
    doc.setOcurrioEn(Instant.now());

    switch (solicitud) {
      case ConfirmacionRecepcion confirmacion -> {
        doc.setTipoTransicion(TipoTransicionEntrega.CONFIRMAR_ENTREGA);
        doc.setFotoRecepcionUrl(confirmacion.fotoRecepcionUrl());
      }
      case NoRecepcion noRecepcion -> {
        doc.setTipoTransicion(TipoTransicionEntrega.NO_RECEPCION);
        doc.setJustificacion(noRecepcion.justificacion());
        doc.setReplanificable(noRecepcion.replanificable());
      }
      case RevisionEntrega ignored -> doc.setTipoTransicion(TipoTransicionEntrega.REVISION);
      case RegresoDeposito ignored -> doc.setTipoTransicion(TipoTransicionEntrega.REGRESO_DEPOSITO);
    }

    springDataRepo.save(doc);
  }
}
