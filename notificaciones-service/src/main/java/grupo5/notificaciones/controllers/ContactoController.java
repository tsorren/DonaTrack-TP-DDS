package grupo5.notificaciones.controllers;

import static grupo5.notificaciones.seguridad.Acceso.ADMIN;
import static grupo5.notificaciones.seguridad.Acceso.PUBLICO;

import grupo5.notificaciones.dto.contacto.MensajeContactoDTO;
import grupo5.notificaciones.dto.contacto.MensajeContactoInputDTO;
import grupo5.notificaciones.dto.contacto.NumeroMensajeDTO;
import grupo5.notificaciones.dto.contacto.RespuestaContactoInputDTO;
import grupo5.notificaciones.models.entities.contacto.EstadoMensajeContacto;
import grupo5.notificaciones.services.impl.ContactoService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize(ADMIN)
@RequiredArgsConstructor
public class ContactoController implements IContactoController {

  static final String HEADER_TOTAL = "X-Total-Count";

  private final ContactoService service;

  @Override
  @PreAuthorize(PUBLICO)
  @PostMapping("/api/contacto")
  public ResponseEntity<NumeroMensajeDTO> recibir(
      @RequestHeader(value = "X-Forwarded-For", required = false) String xForwardedFor,
      @Valid @RequestBody MensajeContactoInputDTO dto) {
    ContactoService.Recepcion recepcion = service.recibir(dto, xForwardedFor);
    return ResponseEntity.status(recepcion.nuevo() ? HttpStatus.CREATED : HttpStatus.OK)
        .body(new NumeroMensajeDTO(recepcion.numero()));
  }

  @Override
  @GetMapping("/api/contacto")
  public ResponseEntity<List<MensajeContactoDTO>> listar(
      @RequestParam(required = false) EstadoMensajeContacto estado,
      @RequestParam(defaultValue = "0") int pagina) {
    ContactoService.Pagina resultado = service.listar(estado, pagina);
    return ResponseEntity.ok()
        .header(HEADER_TOTAL, String.valueOf(resultado.total()))
        .body(resultado.mensajes());
  }

  @Override
  @GetMapping("/api/contacto/{numero}")
  public ResponseEntity<MensajeContactoDTO> obtener(@PathVariable long numero) {
    return ResponseEntity.of(service.obtener(numero));
  }

  @Override
  @PostMapping("/api/contacto/{numero}/respuesta")
  public ResponseEntity<MensajeContactoDTO> responder(
      @PathVariable long numero, @Valid @RequestBody RespuestaContactoInputDTO dto) {
    return ResponseEntity.of(service.responder(numero, dto.texto()));
  }
}
