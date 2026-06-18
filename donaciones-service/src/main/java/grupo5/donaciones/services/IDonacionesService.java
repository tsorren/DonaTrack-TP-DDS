package grupo5.donaciones.services;

import grupo5.donaciones.dto.donaciones.inputs.DonacionInputDTO;
import grupo5.donaciones.dto.donaciones.outputs.DonacionOutputDTO;

public interface IDonacionesService {
  DonacionOutputDTO cargarDonacion(DonacionInputDTO dto);
}
