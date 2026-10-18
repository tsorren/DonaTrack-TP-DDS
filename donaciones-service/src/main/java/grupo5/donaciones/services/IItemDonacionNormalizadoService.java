package grupo5.donaciones.services;

import grupo5.donaciones.dto.itemsNormalizados.inputs.ItemDonacionNormalizadoPatchDTO;
import grupo5.donaciones.dto.itemsNormalizados.outputs.ItemDonacionNormalizadoOutputDTO;
import java.util.List;
import java.util.UUID;

public interface IItemDonacionNormalizadoService {
  List<ItemDonacionNormalizadoOutputDTO> obtenerPendientes();

  ItemDonacionNormalizadoOutputDTO obtener(UUID id);

  ItemDonacionNormalizadoOutputDTO actualizarEstado(UUID id, ItemDonacionNormalizadoPatchDTO dto);
}
