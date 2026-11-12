package grupo5.notificaciones.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.notificaciones.dto.input.EventoCuentaEnlaceSolicitadoV1;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MensajeDeCuentaTest {

  private static final String ENLACE =
      "http://localhost:8180/realms/donatrack/login-actions/action-token?key=abc&client_id=x";

  private static EventoCuentaEnlaceSolicitadoV1 evento(String tipo) {
    return new EventoCuentaEnlaceSolicitadoV1(
        tipo, UUID.randomUUID(), "ana@example.com", ENLACE, 30, "es", Instant.now());
  }

  @Test
  void recuperarLaClaveLlevaElEnlaceYCuantoVale() {
    String mensaje = MensajeDeCuenta.armar(evento(EventoCuentaEnlaceSolicitadoV1.RECUPERAR_CLAVE));

    assertTrue(mensaje.startsWith("Recuperá tu contraseña de DonaTrack"), mensaje);
    assertTrue(mensaje.contains(ENLACE), mensaje);
    assertTrue(mensaje.contains("vale 30 minutos"), mensaje);
    assertTrue(mensaje.contains("una sola vez"), mensaje);
    assertTrue(mensaje.contains("ignorá este mensaje"), mensaje);
  }

  @Test
  void verificarElEmailLlevaElEnlaceYCuantoVale() {
    String mensaje = MensajeDeCuenta.armar(evento(EventoCuentaEnlaceSolicitadoV1.VERIFICAR_EMAIL));

    assertTrue(mensaje.startsWith("Verificá tu email en DonaTrack"), mensaje);
    assertTrue(mensaje.contains(ENLACE), mensaje);
    assertTrue(mensaje.contains("vale 30 minutos"), mensaje);
  }

  @Test
  void elVencimientoSaleDelEvento() {
    EventoCuentaEnlaceSolicitadoV1 de60 =
        new EventoCuentaEnlaceSolicitadoV1(
            EventoCuentaEnlaceSolicitadoV1.RECUPERAR_CLAVE,
            UUID.randomUUID(),
            "ana@example.com",
            ENLACE,
            60,
            "es",
            Instant.now());

    assertTrue(MensajeDeCuenta.armar(de60).contains("vale 60 minutos"));
  }

  @Test
  void elMensajeNoLlevaElIdDeLaCuentaNiElEmailDelDestinatario() {
    EventoCuentaEnlaceSolicitadoV1 evento = evento(EventoCuentaEnlaceSolicitadoV1.RECUPERAR_CLAVE);

    String mensaje = MensajeDeCuenta.armar(evento);

    assertFalse(mensaje.contains(evento.cuentaId().toString()), mensaje);
    assertFalse(mensaje.contains("ana@example.com"), mensaje);
  }

  @Test
  void unTipoDesconocidoNoSeArma() {
    assertThrows(IllegalArgumentException.class, () -> MensajeDeCuenta.armar(evento("OTRO")));
  }

  @Test
  void elEnlaceViajaTalCualSinCodificar() {
    assertEquals(
        1,
        MensajeDeCuenta.armar(evento(EventoCuentaEnlaceSolicitadoV1.VERIFICAR_EMAIL))
                .split(java.util.regex.Pattern.quote(ENLACE), -1)
                .length
            - 1);
  }
}
