package grupo5.incentivos.services;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.incentivos.dto.InsigniaDTO;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
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
    return obtenerDonante(donanteId).getInsignias().stream().map(InsigniaDTO::desde).toList();
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
