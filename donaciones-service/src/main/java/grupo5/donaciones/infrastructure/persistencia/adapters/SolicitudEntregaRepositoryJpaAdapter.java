package grupo5.donaciones.infrastructure.persistencia.adapters;

import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.donaciones.infrastructure.persistencia.entities.logistica.SolicitudEntregaEntity;
import grupo5.donaciones.infrastructure.persistencia.mapper.LogisticaPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataSolicitudEntregaRepository;
import grupo5.donaciones.models.entities.logistica.EstadoSolicitudEntrega;
import grupo5.donaciones.models.entities.logistica.SolicitudEntrega;
import grupo5.donaciones.models.repositories.ISolicitudesEntregaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("postgres")
public class SolicitudEntregaRepositoryJpaAdapter
    extends CrudRepositoryJpaAdapter<
        SolicitudEntrega, SolicitudEntregaEntity, SpringDataSolicitudEntregaRepository>
    implements ISolicitudesEntregaRepository {

  public SolicitudEntregaRepositoryJpaAdapter(
      SpringDataSolicitudEntregaRepository springDataRepo, LogisticaPersistenciaMapper mapper) {
    super(springDataRepo, mapper::toEntity, mapper::toDomain);
  }

  @Override
  public Optional<SolicitudEntrega> findActivaPorDonacion(UUID donacionIndependienteId) {
    return springDataRepo
        .findFirstByDonacionIndependienteIdAndEstadoNot(
            donacionIndependienteId, EstadoSolicitudEntrega.FALLIDA)
        .map(toDomain);
  }

  @Override
  public Optional<SolicitudEntrega> findMasRecientePorDonacion(UUID donacionIndependienteId) {
    return springDataRepo
        .findFirstByDonacionIndependienteIdOrderByFechaCreacionDesc(donacionIndependienteId)
        .map(toDomain);
  }
}
