package grupo5.donaciones.services;

import grupo5.donaciones.dto.donantes.ArchivoInputDTO;
import grupo5.donaciones.dto.donantes.ArchivoOutputDTO;
import java.util.UUID;

public interface IArchivoDonantesService {
  ArchivoOutputDTO cargarArchivoDonantes(ArchivoInputDTO input);

  ArchivoOutputDTO obtenerPorId(UUID id);
}
