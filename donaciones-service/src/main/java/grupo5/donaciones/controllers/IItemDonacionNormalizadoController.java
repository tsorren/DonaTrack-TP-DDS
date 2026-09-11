package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.itemsNormalizados.inputs.ItemDonacionNormalizadoPatchDTO;
import grupo5.donaciones.dto.itemsNormalizados.outputs.ItemDonacionNormalizadoOutputDTO;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

public interface IItemDonacionNormalizadoController {
  ResponseEntity<List<ItemDonacionNormalizadoOutputDTO>> obtenerPendientes();

  ResponseEntity<ItemDonacionNormalizadoOutputDTO> obtenerPorId(UUID id);

  ResponseEntity<ItemDonacionNormalizadoOutputDTO> actualizarEstado(
      UUID id, ItemDonacionNormalizadoPatchDTO dto);
}
