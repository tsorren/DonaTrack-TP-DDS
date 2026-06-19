package grupo5.donaciones.services;

import grupo5.donaciones.dto.donantes.DonanteInputDTO;
import grupo5.donaciones.dto.donantes.DonanteOutputDTO;
import java.util.List;
import java.util.UUID;

public interface IDonantesService {
  DonanteOutputDTO crearDonante(DonanteInputDTO dto);

  List<DonanteOutputDTO> listarDonantesPorContacto(String canal);

  DonanteOutputDTO obtenerPorId(UUID id);

  DonanteOutputDTO actualizarCanal(UUID id, DonanteInputDTO dto);

  void eliminarDonante(UUID id);
}
