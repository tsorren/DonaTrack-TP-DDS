package grupo5.donaciones.services;

import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.donantes.ArchivoDonantesSubido;
import grupo5.donaciones.dto.donantes.ArchivoOutputDTO;
import grupo5.donaciones.dto.donantes.FilaConErrorDTO;
import grupo5.donaciones.dto.donantes.FiltroArchivos;
import java.util.UUID;

public interface IArchivoDonantesService {

  ArchivoOutputDTO cargarArchivoDonantes(ArchivoDonantesSubido archivo);

  ArchivoOutputDTO obtenerPorId(UUID id);

  /**
   * Filas con error del padrón, ordenadas por línea; el total es el contador {@code conError}.
   * {@code page} es 0-based y {@code size} va de 1 a 100 (20 por defecto).
   */
  Pagina<FilaConErrorDTO> obtenerFilasConError(UUID id, Integer page, Integer size);

  /**
   * Una página de importaciones (R11-20), de la más reciente a la más antigua por defecto ({@code
   * orden}: {@code recientes} o {@code antiguas}); {@code q} busca en el nombre con que se subió.
   * El total es el de las que cumplen la búsqueda y los filtros. {@code desde} posterior a {@code
   * hasta}: 400.
   */
  Pagina<ArchivoOutputDTO> listar(FiltroArchivos filtro, ParametrosListado listado);
}
