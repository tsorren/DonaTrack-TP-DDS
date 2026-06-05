package grupo5.incentivos.models.repositories;

import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class DonanteIncentivosRepository {

  private final Map<Long, DonanteIncentivos> store = new ConcurrentHashMap<>();

  public DonanteIncentivos guardar(DonanteIncentivos donante) {
    this.store.put(donante.getDonanteId(), donante);
    return donante;
  }

  public Optional<DonanteIncentivos> buscarPorId(Long donanteId) {
    return Optional.ofNullable(this.store.get(donanteId));
  }

  public List<DonanteIncentivos> listarTodos() {
    return new ArrayList<>(this.store.values());
  }

  public boolean existe(Long donanteId) {
    return this.store.containsKey(donanteId);
  }
}
