package grupo5.incentivos.services;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.incentivos.dto.InsigniaDTO;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.insignias.InsigniaGanada;
import grupo5.incentivos.models.repositories.IDonanteIncentivosRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InsigniasService implements IInsigniasService {

  private final IDonanteIncentivosRepository repository;

  public InsigniasService(IDonanteIncentivosRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<InsigniaDTO> obtenerInsignias(UUID donanteId) {
    return obtenerInsignias(donanteId, null);
  }

  @Override
  public List<InsigniaDTO> obtenerInsignias(UUID donanteId, Boolean soloVisibles) {
    DonanteIncentivos donante = obtenerDonante(donanteId);
    List<InsigniaGanada> insignias =
        Boolean.TRUE.equals(soloVisibles) ? donante.insigniasVisibles() : donante.getInsignias();
    return insignias.stream().map(InsigniaDTO::desde).toList();
  }

  @Override
  public void configurarVisibilidadInsignia(
      UUID donanteId, String nombreInsignia, boolean visible) {
    DonanteIncentivos donante = obtenerDonante(donanteId);
    donante.configurarVisibilidadInsignia(nombreInsignia, visible);
    repository.save(donante);
  }

  private DonanteIncentivos obtenerDonante(UUID donanteId) {
    return repository
        .findById(donanteId)
        .orElseThrow(
            () -> new BusinessStateException(ErrorCatalog.DONANTE_INCENTIVOS_NO_ENCONTRADO));
  }
}
