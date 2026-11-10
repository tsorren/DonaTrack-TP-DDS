package grupo5.donaciones.infrastructure.persistencia.mapper;

import grupo5.donaciones.infrastructure.persistencia.entities.donantes.ArchivoEntity;
import grupo5.donaciones.infrastructure.persistencia.entities.donantes.ArchivoFilaErrorEntity;
import grupo5.donaciones.models.entities.donantes.Archivo;
import grupo5.donaciones.models.entities.donantes.FilaConError;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("postgres")
public class ArchivosPersistenciaMapper {

  public ArchivoEntity toEntity(Archivo domain) {
    ArchivoEntity entity = new ArchivoEntity();
    entity.setId(domain.getId());
    entity.setPath(domain.getPath());
    entity.setEstado(domain.getEstado());
    entity.setTotalFilas(domain.getTotalFilas());
    entity.setCreadas(domain.getCreadas());
    entity.setActualizadas(domain.getActualizadas());
    entity.setConError(domain.getConError());
    return entity;
  }

  public Archivo toDomain(ArchivoEntity entity) {
    return new Archivo(
        entity.getId(),
        entity.getPath(),
        entity.getEstado(),
        entity.getTotalFilas(),
        entity.getCreadas(),
        entity.getActualizadas(),
        entity.getConError());
  }

  public ArchivoFilaErrorEntity filaToEntity(FilaConError fila) {
    ArchivoFilaErrorEntity entity = new ArchivoFilaErrorEntity();
    entity.setArchivoId(fila.archivoId());
    entity.setLinea(fila.linea());
    entity.setMotivo(fila.motivo());
    entity.setCodigo(fila.codigo());
    return entity;
  }

  public FilaConError filaToDomain(ArchivoFilaErrorEntity entity) {
    return new FilaConError(
        entity.getArchivoId(), entity.getLinea(), entity.getMotivo(), entity.getCodigo());
  }
}
