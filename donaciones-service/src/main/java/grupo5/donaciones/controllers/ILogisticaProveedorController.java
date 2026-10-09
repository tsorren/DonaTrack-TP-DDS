package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.logistica.PreferenciaProveedorRequestDTO;
import grupo5.donaciones.dto.logistica.ProveedorLogisticaDTO;
import java.util.List;
import org.springframework.http.ResponseEntity;

public interface ILogisticaProveedorController {
  ResponseEntity<List<ProveedorLogisticaDTO>> listarProveedores();

  ResponseEntity<List<ProveedorLogisticaDTO>> cambiarProveedorPreferido(
      PreferenciaProveedorRequestDTO request);
}
