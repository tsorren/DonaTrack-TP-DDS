package grupo5.donaciones.infrastructure.persistencia.mapper;

import grupo5.donaciones.infrastructure.persistencia.entities.propuestas.PosibleFragmentacionEntity;
import grupo5.donaciones.infrastructure.persistencia.entities.propuestas.PropuestaEntity;
import grupo5.donaciones.models.entities.propuestas.PosibleFragmentacion;
import grupo5.donaciones.models.entities.propuestas.Propuesta;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PropuestasPersistenciaMapper {

  /** {@code existente}: la fila ya guardada, para que orphanRemoval borre las fragmentaciones. */
  public PropuestaEntity toEntity(Propuesta domain, PropuestaEntity existente) {
    PropuestaEntity entity = existente != null ? existente : new PropuestaEntity();
    entity.setId(domain.getId());
    entity.setNecesidadId(domain.getNecesidadQueSatisfaceId());
    entity.setEstado(domain.getEstado());
    entity.setFechaCreacion(domain.getFechaCreacion());
    // Las fragmentaciones no tienen identidad en el dominio: se reemplazan
    entity.getFragmentaciones().clear();
    for (PosibleFragmentacion f : domain.getPosiblesFragmentaciones()) {
      PosibleFragmentacionEntity fila = new PosibleFragmentacionEntity();
      fila.setPropuesta(entity);
      fila.setDonacionIndependienteId(f.getDonacionOriginalId());
      fila.setCantidad(f.getCantidadNecesaria());
      entity.getFragmentaciones().add(fila);
    }
    return entity;
  }

  public Propuesta toDomain(PropuestaEntity entity) {
    List<PosibleFragmentacion> fragmentaciones =
        entity.getFragmentaciones().stream()
            .map(f -> new PosibleFragmentacion(f.getDonacionIndependienteId(), f.getCantidad()))
            .toList();
    return new Propuesta(
        entity.getId(),
        entity.getNecesidadId(),
        entity.getEstado(),
        entity.getFechaCreacion(),
        fragmentaciones);
  }
}
