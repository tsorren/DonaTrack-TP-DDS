package grupo5.donaciones.controllers.impl;

import grupo5.donaciones.controllers.ILogisticaProveedorController;
import grupo5.donaciones.dto.logistica.PreferenciaProveedorRequestDTO;
import grupo5.donaciones.dto.logistica.ProveedorLogisticaDTO;
import grupo5.donaciones.services.IAdministracionProveedoresService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administración del broker de logística: ver los proveedores y cambiar el preferido en caliente.
 */
@RestController
@RequestMapping("/api/logistica")
public class LogisticaProveedorController implements ILogisticaProveedorController {

  private final IAdministracionProveedoresService administracionService;

  public LogisticaProveedorController(IAdministracionProveedoresService administracionService) {
    this.administracionService = administracionService;
  }

  @Override
  @GetMapping("/proveedores")
  public ResponseEntity<List<ProveedorLogisticaDTO>> listarProveedores() {
    return ResponseEntity.ok(administracionService.listarProveedores());
  }

  @Override
  @PutMapping("/proveedor-preferido")
  public ResponseEntity<List<ProveedorLogisticaDTO>> cambiarProveedorPreferido(
      @Valid @RequestBody PreferenciaProveedorRequestDTO request) {
    return ResponseEntity.ok(
        administracionService.cambiarProveedorPreferido(request.proveedorId()));
  }
}
