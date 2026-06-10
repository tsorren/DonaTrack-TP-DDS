package grupo5.donaciones.repositories;

import grupo5.donaciones.dto.donantes.DonanteDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class DonantesRepository {

  private final Map<Long, DonanteDTO> donantes = new ConcurrentHashMap<>();

  public List<DonanteDTO> buscarTodos() {
    return new ArrayList<>(donantes.values());
  }

  public Optional<DonanteDTO> buscarPorId(Long id) {
    return Optional.ofNullable(donantes.get(id));
  }

  public DonanteDTO guardar(DonanteDTO donante) {
    donantes.put(donante.id(), donante);
    return donante;
  }

  public void eliminarPorId(Long id) {
    donantes.remove(id);
  }

  public boolean existePorId(Long id) {
    return donantes.containsKey(id);
  }
}
