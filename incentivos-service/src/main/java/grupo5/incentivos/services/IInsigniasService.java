package grupo5.incentivos.services;

import grupo5.incentivos.dto.InsigniaDTO;
import java.util.List;
import java.util.UUID;

public interface IInsigniasService {

  List<InsigniaDTO> obtenerInsignias(UUID donanteId);

  void configurarVisibilidadInsignia(UUID donanteId, String nombreInsignia, boolean visible);
}
