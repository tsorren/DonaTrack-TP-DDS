package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.fotos.SolicitudSubidaFotoInputDTO;
import grupo5.donaciones.dto.fotos.SubidaFotoOutputDTO;
import org.springframework.http.ResponseEntity;

public interface IFotosBienesController {
  ResponseEntity<SubidaFotoOutputDTO> solicitarSubidaFoto(SolicitudSubidaFotoInputDTO solicitud);
}
