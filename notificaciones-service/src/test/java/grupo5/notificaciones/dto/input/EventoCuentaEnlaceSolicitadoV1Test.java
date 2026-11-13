package grupo5.notificaciones.dto.input;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventoCuentaEnlaceSolicitadoV1Test {

  @Test
  void elToStringNoMuestraElEmailNiElEnlace() {
    UUID cuenta = UUID.randomUUID();
    EventoCuentaEnlaceSolicitadoV1 evento =
        new EventoCuentaEnlaceSolicitadoV1(
            EventoCuentaEnlaceSolicitadoV1.RECUPERAR_CLAVE,
            cuenta,
            "ana@example.com",
            "http://localhost:8180/realms/donatrack/login-actions/action-token?key=SECRETO",
            30,
            "es",
            Instant.now());

    String texto = evento.toString();

    assertFalse(texto.contains("ana@example.com"), texto);
    assertFalse(texto.contains("SECRETO"), texto);
    assertFalse(texto.contains("action-token"), texto);
    assertTrue(texto.contains(cuenta.toString()), texto);
    assertTrue(texto.contains("RECUPERAR_CLAVE"), texto);
  }
}
