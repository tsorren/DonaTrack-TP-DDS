package grupo5.donaciones.controllers.impl;

import grupo5.donaciones.controllers.IFotosBienesController;
import grupo5.donaciones.dto.fotos.SolicitudSubidaFotoInputDTO;
import grupo5.donaciones.dto.fotos.SubidaFotoOutputDTO;
import grupo5.donaciones.services.IFotosBienesService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Entrega la URL prefirmada para subir la foto de un bien. El cliente sube directo al
 * almacenamiento (el servicio no maneja los bytes) y manda la referencia en {@code fotoUrl}.
 */
@RestController
@RequestMapping("/api/donaciones/fotos")
public class FotosBienesController implements IFotosBienesController {

  private final IFotosBienesService service;

  public FotosBienesController(IFotosBienesService service) {
    this.service = service;
  }

  @Override
  @PostMapping
  public ResponseEntity<SubidaFotoOutputDTO> solicitarSubidaFoto(
      @Valid @RequestBody SolicitudSubidaFotoInputDTO solicitud) {
    return ResponseEntity.ok(service.solicitarSubida(solicitud));
  }
}
