package grupo5.notificaciones.controllers;

import grupo5.notificaciones.dto.contacto.MensajeContactoDTO;
import grupo5.notificaciones.dto.contacto.MensajeContactoInputDTO;
import grupo5.notificaciones.dto.contacto.NumeroMensajeDTO;
import grupo5.notificaciones.dto.contacto.RespuestaContactoInputDTO;
import grupo5.notificaciones.models.entities.contacto.EstadoMensajeContacto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Contacto", description = "Mensajes del formulario de contacto (HB-21)")
public interface IContactoController {

  @Operation(
      summary = "Recibir un mensaje del formulario de contacto",
      description =
          "Público. La misma claveEnvio otra vez devuelve 200 con el número del primero, antes de"
              + " mirar el límite. X-Forwarded-For: la primera IP es la del navegador.")
  @ApiResponse(responseCode = "201", description = "Mensaje recibido")
  @ApiResponse(responseCode = "200", description = "La clave ya estaba: el número del primero")
  @ApiResponse(responseCode = "400", description = "Datos inválidos (ERR-CSR-003)")
  @ApiResponse(responseCode = "429", description = "Límite de envíos superado (ERR-EST-903)")
  @PostMapping("/api/contacto")
  ResponseEntity<NumeroMensajeDTO> recibir(
      @RequestHeader(value = "X-Forwarded-For", required = false) String xForwardedFor,
      @Valid @RequestBody MensajeContactoInputDTO dto);

  @Operation(
      summary = "Listar los mensajes de contacto (ADMIN)",
      description = "Del más nuevo al más viejo, 20 por página; el total va en X-Total-Count.")
  @ApiResponse(responseCode = "200", description = "Página de mensajes")
  @ApiResponse(responseCode = "400", description = "Estado o página inválidos")
  @GetMapping("/api/contacto")
  ResponseEntity<List<MensajeContactoDTO>> listar(
      @RequestParam(required = false) EstadoMensajeContacto estado,
      @RequestParam(defaultValue = "0") int pagina);

  @Operation(summary = "Obtener un mensaje de contacto (ADMIN)")
  @ApiResponse(responseCode = "200", description = "Mensaje encontrado")
  @ApiResponse(responseCode = "404", description = "No hay mensaje con ese número")
  @GetMapping("/api/contacto/{numero}")
  ResponseEntity<MensajeContactoDTO> obtener(@PathVariable long numero);

  @Operation(
      summary = "Responder un mensaje de contacto por correo (ADMIN)",
      description = "RESPONDIDO si el correo salió; ENVIO_FALLIDO si falló el proveedor.")
  @ApiResponse(responseCode = "200", description = "Mensaje con su nuevo estado")
  @ApiResponse(responseCode = "400", description = "Respuesta vacía o demasiado larga")
  @ApiResponse(responseCode = "404", description = "No hay mensaje con ese número")
  @PostMapping("/api/contacto/{numero}/respuesta")
  ResponseEntity<MensajeContactoDTO> responder(
      @PathVariable long numero, @Valid @RequestBody RespuestaContactoInputDTO dto);
}
