package grupo5.donaciones.controllers.impl;

import grupo5.donaciones.controllers.IItemDonacionNormalizadoController;
import grupo5.donaciones.dto.itemsNormalizados.inputs.ItemDonacionNormalizadoPatchDTO;
import grupo5.donaciones.dto.itemsNormalizados.outputs.ItemDonacionNormalizadoOutputDTO;
import grupo5.donaciones.services.IItemDonacionNormalizadoService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/items-normalizados")
public class ItemDonacionNormalizadoController implements IItemDonacionNormalizadoController {

  private final IItemDonacionNormalizadoService service;

  public ItemDonacionNormalizadoController(IItemDonacionNormalizadoService service) {
    this.service = service;
  }

  @Override
  @GetMapping("/pendientes")
  public ResponseEntity<List<ItemDonacionNormalizadoOutputDTO>> obtenerPendientes() {
    return ResponseEntity.ok(service.obtenerPendientes());
  }

  @Override
  @GetMapping("/{id}")
  public ResponseEntity<ItemDonacionNormalizadoOutputDTO> obtenerPorId(@PathVariable UUID id) {
    return ResponseEntity.ok(service.obtener(id));
  }

  @Override
  @PatchMapping("/{id}")
  public ResponseEntity<ItemDonacionNormalizadoOutputDTO> actualizarEstado(
      @PathVariable UUID id, @Valid @RequestBody ItemDonacionNormalizadoPatchDTO dto) {
    return ResponseEntity.ok(service.actualizarEstado(id, dto));
  }
}
