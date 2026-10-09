package grupo5.logistica.infrastructure.persistencia.adapters;

import grupo5.logistica.infrastructure.persistencia.entities.SolicitudTransicionEntregaEntity;
import grupo5.logistica.infrastructure.persistencia.entities.TipoTransicionEntrega;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataSolicitudTransicionEntregaRepository;
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
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("postgres")
@Transactional
public class SolicitudesTransicionEntregaRepositoryJpaAdapter
    implements ISolicitudesTransicionEntregaRepository {

  private final SpringDataSolicitudTransicionEntregaRepository springDataRepo;

  public SolicitudesTransicionEntregaRepositoryJpaAdapter(
      SpringDataSolicitudTransicionEntregaRepository springDataRepo) {
    this.springDataRepo = springDataRepo;
  }

  @Override
  public void registrar(SolicitudTransicionEntrega solicitud) {
    if (solicitud == null || solicitud.entrega() == null) {
      return;
    }

    SolicitudTransicionEntregaEntity entity = new SolicitudTransicionEntregaEntity();
    entity.setIdSolicitud(UUID.randomUUID());
    entity.setIdEntrega(solicitud.entrega().getId());
    entity.setActor(solicitud.actor());
    entity.setOcurrioEn(Instant.now());

    switch (solicitud) {
      case ConfirmacionRecepcion confirmacion -> {
        entity.setTipoTransicion(TipoTransicionEntrega.CONFIRMAR_ENTREGA);
        entity.setFotoRecepcionUrl(confirmacion.fotoRecepcionUrl());
      }
      case NoRecepcion noRecepcion -> {
        entity.setTipoTransicion(TipoTransicionEntrega.NO_RECEPCION);
        entity.setJustificacion(noRecepcion.justificacion());
        entity.setReplanificable(noRecepcion.replanificable());
      }
      case RevisionEntrega ignored -> entity.setTipoTransicion(TipoTransicionEntrega.REVISION);
      case RegresoDeposito ignored ->
          entity.setTipoTransicion(TipoTransicionEntrega.REGRESO_DEPOSITO);
    }

    springDataRepo.save(entity);
  }
}
