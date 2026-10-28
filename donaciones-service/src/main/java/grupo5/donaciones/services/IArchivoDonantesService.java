package grupo5.donaciones.services;

import grupo5.donaciones.dto.donantes.ArchivoDonantesSubido;
import grupo5.donaciones.dto.donantes.ArchivoOutputDTO;
import java.util.UUID;

public interface IArchivoDonantesService {
  ArchivoOutputDTO cargarArchivoDonantes(ArchivoDonantesSubido archivo);

  ArchivoOutputDTO obtenerPorId(UUID id);
}
