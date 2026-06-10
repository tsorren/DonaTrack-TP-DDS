package grupo5.donaciones.repositories;

import grupo5.donaciones.dto.necesidades.NecesidadDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class NecesidadesRepository {

  private final Map<Long, NecesidadDTO> necesidades = new ConcurrentHashMap<>();

  public List<NecesidadDTO> buscarTodas() {
    return new ArrayList<>(necesidades.values());
  }

  public Optional<NecesidadDTO> buscarPorId(Long id) {
    return Optional.ofNullable(necesidades.get(id));
  }

  public NecesidadDTO guardar(NecesidadDTO necesidad) {
    necesidades.put(necesidad.id(), necesidad);
    return necesidad;
  }

  public void eliminarPorId(Long id) {
    necesidades.remove(id);
  }
}
