package grupo5.notificaciones.controllers;

import grupo5.notificaciones.dto.input.*;
import grupo5.notificaciones.models.entities.notificaciones.eventos.*;
import grupo5.notificaciones.services.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {
  private final NotificacionService service;

  @PostMapping("/donantes/registrados")
  public ResponseEntity<Void> registrarDonante(@RequestBody EventoDonanteRegistradoDTO dto) {

    service.procesar(
        new DonanteRegistrado(dto.getPersona(), dto.getCredencialesDeAcceso(), dto.getFecha()));

    return ResponseEntity.ok().build();
  }

  @PostMapping("/donantes/inactivos")
  public ResponseEntity<Void> donanteInactivo(@RequestBody EventoDonanteInactivoDTO dto) {

    service.procesar(new DonanteInactivo(dto.getPersona(), dto.getDiasInactivo(), dto.getFecha()));
    return ResponseEntity.ok().build();
  }

  @PostMapping("/misiones/cumplidas")
  public ResponseEntity<Void> misionCumplida(@RequestBody EventoMisionCumplidaDTO dto) {

    service.procesar(
        new MisionCumplida(
            dto.getPersona(), dto.getNombreMision(), dto.getRecompensa(), dto.getFecha()));
    return ResponseEntity.ok().build();
  }

  @PostMapping("/categorias/cambios")
  public ResponseEntity<Void> cambioCategoria(@RequestBody EventoSubioCategoriaDTO dto) {

    service.procesar(
        new SubioCategoria(
            dto.getPersona(), dto.getCategoriaVieja(), dto.getCategoriaNueva(), dto.getFecha()));
    return ResponseEntity.ok().build();
  }

  @PostMapping("/donaciones/asignadas")
  public ResponseEntity<Void> donacionAsignada(@RequestBody EventoDonacionDTO dto) {

    service.procesar(
        new DonacionAsignada(
            dto.getPersona(),
            dto.getEntidadBeneficiaria(),
            dto.getDetalleDonacion(),
            dto.getFecha()));

    service.procesar(
        new DonacionRecibida(
            dto.getPersona(),
            dto.getEntidadBeneficiaria(),
            dto.getDetalleDonacion(),
            dto.getFecha()));

    return ResponseEntity.ok().build();
  }
}
