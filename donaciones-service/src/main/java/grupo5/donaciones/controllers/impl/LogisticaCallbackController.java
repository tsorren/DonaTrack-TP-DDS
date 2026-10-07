package grupo5.donaciones.controllers.impl;

import grupo5.donaciones.controllers.ILogisticaCallbackController;
import grupo5.donaciones.dto.logistica.AvisoProveedorRequestDTO;
import grupo5.donaciones.services.IAvisosProveedorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Callback por el que un proveedor de logística HTTP avisa cómo va una entrega. */
@RestController
@RequestMapping("/api/logistica/proveedores")
public class LogisticaCallbackController implements ILogisticaCallbackController {

  private final IAvisosProveedorService avisosProveedorService;

  public LogisticaCallbackController(IAvisosProveedorService avisosProveedorService) {
    this.avisosProveedorService = avisosProveedorService;
  }

  @Override
  @PostMapping("/{proveedorId}/avisos")
  public ResponseEntity<Void> recibirAviso(
      @PathVariable String proveedorId, @Valid @RequestBody AvisoProveedorRequestDTO aviso) {
    avisosProveedorService.registrarAviso(proveedorId, aviso);
    return ResponseEntity.accepted().build();
  }
}
