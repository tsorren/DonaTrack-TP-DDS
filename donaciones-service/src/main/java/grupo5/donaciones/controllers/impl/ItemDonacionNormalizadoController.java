package grupo5.donaciones.controllers.impl;

import grupo5.donaciones.controllers.IItemDonacionNormalizadoController;
import grupo5.donaciones.dto.itemsNormalizados.inputs.ItemDonacionNormalizadoPatchDTO;
import grupo5.donaciones.dto.itemsNormalizados.outputs.ItemDonacionNormalizadoOutputDTO;
import grupo5.donaciones.services.IItemDonacionNormalizadoService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${donatrack.routes.donaciones.items-normalizados-base}")
public class ItemDonacionNormalizadoController implements IItemDonacionNormalizadoController {

  private final IItemDonacionNormalizadoService service;

  public ItemDonacionNormalizadoController(IItemDonacionNormalizadoService service) {
    this.service = service;
  }

  @Override
  @GetMapping("${donatrack.routes.donaciones.items-normalizados-pendientes}")
  public ResponseEntity<List<ItemDonacionNormalizadoOutputDTO>> obtenerPendientes() {
    return ResponseEntity.ok(service.obtenerPendientes());
  }

  @Override
  @PatchMapping("${donatrack.routes.donaciones.items-normalizados-id}")
  public ResponseEntity<ItemDonacionNormalizadoOutputDTO> actualizarEstado(
      @PathVariable UUID id, @RequestBody ItemDonacionNormalizadoPatchDTO dto) {
    return ResponseEntity.ok(service.actualizarEstado(id, dto));
  }
}
