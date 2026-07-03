package grupo5.donaciones.services;

import grupo5.donaciones.dto.donaciones.inputs.DonacionInputDTO;
import grupo5.donaciones.dto.donaciones.outputs.DonacionOutputDTO;
import java.util.List;
import java.util.UUID;

public interface IDonacionesService {
  DonacionOutputDTO cargarDonacion(DonacionInputDTO dto);

  List<DonacionOutputDTO> listarDonaciones();

  DonacionOutputDTO obtenerDonacion(UUID id);
}
