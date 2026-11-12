package grupo5.donaciones.infrastructure.persistencia.mapper;

import grupo5.donaciones.infrastructure.persistencia.entities.donantes.ArchivoEntity;
import grupo5.donaciones.infrastructure.persistencia.entities.donantes.ArchivoFilaErrorEntity;
import grupo5.donaciones.models.entities.donantes.Archivo;
import grupo5.donaciones.models.entities.donantes.FilaConError;
import grupo5.donaciones.models.entities.donantes.UltimoImportado;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("postgres")
public class ArchivosPersistenciaMapper {

  private static final String SEPARADOR = "\t";
  private static final String FIN_DE_LINEA = "\n";

  public ArchivoEntity toEntity(Archivo domain) {
    ArchivoEntity entity = new ArchivoEntity();
    entity.setId(domain.getId());
    entity.setPath(domain.getPath());
    entity.setEstado(domain.getEstado());
    entity.setTotalFilas(domain.getTotalFilas());
    entity.setCreadas(domain.getCreadas());
    entity.setActualizadas(domain.getActualizadas());
    entity.setConError(domain.getConError());
    entity.setUltimos(ultimosAColumna(domain.getUltimos()));
    return entity;
  }

  public Archivo toDomain(ArchivoEntity entity) {
    Archivo archivo =
        new Archivo(
            entity.getId(),
            entity.getPath(),
            entity.getEstado(),
            entity.getTotalFilas(),
            entity.getCreadas(),
            entity.getActualizadas(),
            entity.getConError());
    archivo.restaurarUltimos(ultimosDeColumna(entity.getUltimos()));
    return archivo;
  }

  /** Una línea por donante: el resultado, un tabulador y el nombre (sin saltos ni tabuladores). */
  static String ultimosAColumna(List<UltimoImportado> ultimos) {
    if (ultimos.isEmpty()) {
      return null;
    }
    return ultimos.stream()
        .map(u -> u.resultado().name() + SEPARADOR + limpiar(u.nombre()))
        .collect(Collectors.joining(FIN_DE_LINEA));
  }

  static List<UltimoImportado> ultimosDeColumna(String columna) {
    if (columna == null || columna.isBlank()) {
      return List.of();
    }
    return columna
        .lines()
        .map(linea -> linea.split(SEPARADOR, 2))
        .filter(partes -> partes.length == 2)
        .map(partes -> new UltimoImportado(partes[1], UltimoImportado.Resultado.valueOf(partes[0])))
        .toList();
  }

  private static String limpiar(String nombre) {
    return nombre == null ? "" : nombre.replaceAll("[\\t\\r\\n]+", " ").trim();
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
