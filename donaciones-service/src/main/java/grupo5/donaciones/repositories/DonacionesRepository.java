package grupo5.donaciones.repositories;

import grupo5.donaciones.dto.donaciones.DonacionDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class DonacionesRepository {

  private final Map<Long, DonacionDTO> donaciones = new ConcurrentHashMap<>();

  public List<DonacionDTO> buscarTodas() {
    return new ArrayList<>(donaciones.values());
  }

  public Optional<DonacionDTO> buscarPorId(Long id) {
    return Optional.ofNullable(donaciones.get(id));
  }

  public DonacionDTO guardar(DonacionDTO donacion) {
    donaciones.put(donacion.id(), donacion);
    return donacion;
  }

  public void eliminarPorId(Long id) {
    donaciones.remove(id);
  }
}
