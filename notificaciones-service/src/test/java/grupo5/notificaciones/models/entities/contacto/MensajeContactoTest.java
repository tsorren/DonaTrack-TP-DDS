package grupo5.notificaciones.models.entities.contacto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.notificaciones.exceptions.ProveedorMensajeriaException;
import grupo5.notificaciones.models.entities.personas.Correo;
import grupo5.notificaciones.models.ports.NotificacionSender;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MensajeContactoTest {

  private final NotificacionSender sender = mock(NotificacionSender.class);

  private static MensajeContacto mensajeNumero(long numero) {
    MensajeContacto mensaje =
        MensajeContacto.recibido(
            UUID.randomUUID(),
            TemaContacto.ENTREGA,
            "No llegó la entrega",
            null,
            "ana@ejemplo.org",
            "203.0.113.7",
            LocalDateTime.of(2026, 11, 10, 9, 0));
    mensaje.asignarNumero(numero);
    return mensaje;
  }

  @Test
  void recibido_quedaEnRecibidoSinNumeroNiRespuesta() {
    MensajeContacto mensaje =
        MensajeContacto.recibido(
            UUID.randomUUID(), TemaContacto.OTRO, "Hola", null, "a@b.org", null, null);

    assertEquals(EstadoMensajeContacto.RECIBIDO, mensaje.getEstado());
    assertNull(mensaje.getNumero());
    assertNull(mensaje.getRespuesta());
  }

  @Test
  void asignarNumero_dosVeces_falla() {
    MensajeContacto mensaje = mensajeNumero(1);

    assertThrows(IllegalStateException.class, () -> mensaje.asignarNumero(2));
  }

  @Test
  void responder_conEnvioExitoso_quedaRespondidoYMandaAlCorreoDelMensajeConElNumero() {
    MensajeContacto mensaje = mensajeNumero(4821);
    when(sender.enviarA(any(Correo.class), anyString())).thenReturn(true);

    mensaje.responder("Ya la reprogramamos.", sender);

    ArgumentCaptor<Correo> destino = ArgumentCaptor.forClass(Correo.class);
    ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
    verify(sender).enviarA(destino.capture(), texto.capture());
    assertEquals("ana@ejemplo.org", destino.getValue().getDireccionCorreo());
    assertTrue(texto.getValue().startsWith("Respuesta a tu mensaje Nº 4821"));
    assertTrue(texto.getValue().endsWith("Ya la reprogramamos."));
    assertEquals(EstadoMensajeContacto.RESPONDIDO, mensaje.getEstado());
    assertEquals("Ya la reprogramamos.", mensaje.getRespuesta());
    assertNotNull(mensaje.getRespondidoEn());
  }

  @Test
  void responder_conProveedorQueRechaza_quedaEnvioFallidoConLaRespuestaGuardada() {
    MensajeContacto mensaje = mensajeNumero(7);
    when(sender.enviarA(any(Correo.class), anyString())).thenReturn(false);

    mensaje.responder("Respuesta", sender);

    assertEquals(EstadoMensajeContacto.ENVIO_FALLIDO, mensaje.getEstado());
    assertEquals("Respuesta", mensaje.getRespuesta());
    assertNull(mensaje.getRespondidoEn());
  }

  @Test
  void responder_conProveedorQueFalla_quedaEnvioFallidoYSePuedeReintentar() {
    MensajeContacto mensaje = mensajeNumero(8);
    when(sender.enviarA(any(Correo.class), anyString()))
        .thenThrow(new ProveedorMensajeriaException("503"))
        .thenReturn(true);

    mensaje.responder("Respuesta", sender);
    assertEquals(EstadoMensajeContacto.ENVIO_FALLIDO, mensaje.getEstado());

    mensaje.responder("Respuesta", sender);
    assertEquals(EstadoMensajeContacto.RESPONDIDO, mensaje.getEstado());
  }
}
