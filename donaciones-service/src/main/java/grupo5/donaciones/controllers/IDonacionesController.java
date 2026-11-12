package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.donaciones.FiltroDonaciones;
import grupo5.donaciones.dto.donaciones.inputs.DonacionInputDTO;
import grupo5.donaciones.dto.donaciones.outputs.ContadoresPublicosDTO;
import grupo5.donaciones.dto.donaciones.outputs.DonacionOutputDTO;
import grupo5.donaciones.dto.donaciones.outputs.ResumenAdminDTO;
import grupo5.donaciones.services.ParametrosListado;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

public interface IDonacionesController {
  ResponseEntity<DonacionOutputDTO> cargarDonacion(DonacionInputDTO dto);

  ResponseEntity<List<DonacionOutputDTO>> listarDonaciones(
      FiltroDonaciones filtro, ParametrosListado listado);

  ResponseEntity<ResumenAdminDTO> obtenerResumen();

  ResponseEntity<DonacionOutputDTO> obtenerDonacion(UUID id);

  ResponseEntity<ContadoresPublicosDTO> obtenerContadoresPublicos();
}
