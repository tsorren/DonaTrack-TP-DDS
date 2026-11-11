package grupo5.cliente.rest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * Lo que el cliente usa de Notificaciones
 * (docs/arquitectura/contratos/openapi-notificaciones.yaml): la bandeja de una persona, su conteo
 * de sin leer, marcarlas como leídas y el formulario de contacto. {@code personaId} es el id de la
 * persona (el de la cuenta). La declara entera el esqueleto de la conexión de pantallas (SPEC-08):
 * los frentes la usan y no la cambian. Notificaciones no pide token todavía (HB-13, fuera de
 * alcance).
 */
interface NotificacionesApi {

  /** {@code leida}: sin valor, todas; {@code true} o {@code false}, solo esas. */
  @GetExchange("/api/notificaciones/persona/{personaId}")
  List<Notificacion> bandeja(
      @PathVariable UUID personaId, @RequestParam(required = false) Boolean leida);

  @GetExchange("/api/notificaciones/persona/{personaId}/no-leidas/conteo")
  Conteo sinLeer(@PathVariable UUID personaId);

  /** {@code leida=false} la deja sin leer. */
  @PatchExchange("/api/notificaciones/{id}/leida")
  Notificacion marcarLeida(@PathVariable UUID id, @RequestParam boolean leida);

  @PatchExchange("/api/notificaciones/persona/{personaId}/marcar-todas-leidas")
  void marcarTodasLeidas(@PathVariable UUID personaId);

  /**
   * Un mensaje del formulario de contacto (HB-21, ADR {@code proposed} de Notificaciones; contrato
   * fijado en SPEC-08). Público. 201 con el número; la misma {@code claveEnvio} otra vez, 200 con
   * el número del primero; 429 si se pasa del límite. {@code ipOrigen} viaja en {@code
   * X-Forwarded-For}, para el límite por IP: la de quien le habla al cliente liviano, que sin un
   * proxy delante es la del navegador.
   */
  @PostExchange("/api/contacto")
  MensajeRecibido contacto(
      @RequestHeader("X-Forwarded-For") String ipOrigen, @RequestBody MensajeDeContacto mensaje);

  /**
   * {@code estado}: PENDIENTE, ENVIADA o FALLIDA; leída si tiene {@code fechaLectura}. {@code
   * tipoEvento} (DONACION_ASIGNADA, DONACION_EN_CAMINO, DONACION_RECIBIDA, ENTREGA_FALLIDA, etc.) y
   * {@code referenciaId}, el id de la donación, son opcionales (HB-09): vienen nulos en las
   * anteriores y cuando el emisor no los informó.
   */
  record Notificacion(
      UUID id,
      String mensaje,
      String estado,
      LocalDateTime fechaCreacion,
      LocalDateTime fechaLectura,
      String tipoEvento,
      UUID referenciaId) {}

  record Conteo(Long noLeidas) {}

  /**
   * {@code tema}: ENTREGA, DONACION, NECESIDAD, CUENTA, SITIO u OTRO (los seis de la ficha de
   * Contacto); {@code codigoError}, opcional; {@code correo}, obligatorio sin sesión; {@code
   * claveEnvio}, el UUID que el formulario genera al abrirse.
   */
  record MensajeDeContacto(
      String tema, String texto, String codigoError, String correo, UUID claveEnvio) {}

  record MensajeRecibido(Long numero) {}
}
