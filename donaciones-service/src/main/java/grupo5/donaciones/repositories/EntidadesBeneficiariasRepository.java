package grupo5.donaciones.repositories;

import grupo5.donaciones.dto.entidades.EntidadBeneficiariaDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class EntidadesBeneficiariasRepository {

  private final Map<Long, EntidadBeneficiariaDTO> entidadesBeneficiarias =
      new ConcurrentHashMap<>();

  public List<EntidadBeneficiariaDTO> buscarTodas() {
    return new ArrayList<>(entidadesBeneficiarias.values());
  }

  public Optional<EntidadBeneficiariaDTO> buscarPorId(Long id) {
    return Optional.ofNullable(entidadesBeneficiarias.get(id));
  }

  public EntidadBeneficiariaDTO guardar(EntidadBeneficiariaDTO entidadBeneficiaria) {
    entidadesBeneficiarias.put(entidadBeneficiaria.id(), entidadBeneficiaria);
    return entidadBeneficiaria;
  }

  public void eliminarPorId(Long id) {
    entidadesBeneficiarias.remove(id);
  }
}
