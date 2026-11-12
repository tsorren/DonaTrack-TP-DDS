package grupo5.donaciones.services;

import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.donantes.ArchivoDonantesSubido;
import grupo5.donaciones.dto.donantes.ArchivoOutputDTO;
import grupo5.donaciones.dto.donantes.FilaConErrorDTO;
import java.util.UUID;

public interface IArchivoDonantesService {

  ArchivoOutputDTO cargarArchivoDonantes(ArchivoDonantesSubido archivo);

  ArchivoOutputDTO obtenerPorId(UUID id);

  /**
   * Filas con error del padrón, ordenadas por línea; el total es el contador {@code conError}.
   * {@code page} es 0-based y {@code size} va de 1 a 100 (20 por defecto).
   */
  Pagina<FilaConErrorDTO> obtenerFilasConError(UUID id, Integer page, Integer size);
}
