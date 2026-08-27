package grupo5.donaciones.services;

import grupo5.donaciones.dto.donantes.ArchivoInputDTO;
import grupo5.donaciones.dto.donantes.ArchivoOutputDTO;

public interface IArchivoDonantesService {
  ArchivoOutputDTO cargarArchivoDonantes(ArchivoInputDTO input);
}
