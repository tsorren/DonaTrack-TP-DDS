package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.logistica.AvisoProveedorRequestDTO;
import org.springframework.http.ResponseEntity;

public interface ILogisticaCallbackController {
  ResponseEntity<Void> recibirAviso(String proveedorId, AvisoProveedorRequestDTO aviso);
}
