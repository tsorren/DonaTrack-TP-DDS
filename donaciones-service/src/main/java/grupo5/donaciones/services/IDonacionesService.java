package grupo5.donaciones.services;

import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.donaciones.FiltroDonaciones;
import grupo5.donaciones.dto.donaciones.inputs.DonacionInputDTO;
import grupo5.donaciones.dto.donaciones.outputs.DonacionOutputDTO;
import java.util.UUID;

public interface IDonacionesService {
  DonacionOutputDTO cargarDonacion(DonacionInputDTO dto);

  /** Una página de donaciones originales, con la búsqueda y los filtros en la base (R11-01). */
  Pagina<DonacionOutputDTO> buscar(FiltroDonaciones filtro, ParametrosListado listado);

  DonacionOutputDTO obtenerDonacion(UUID id);
}
