package grupo5.auth.correo;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Lo que se puede probar sin un broker: que falle bien. La publicación real la prueba el script
 * E2E.
 */
class PublicadorRabbitMqTest {

  private static final EventoEnlaceDeCuenta EVENTO =
      new EventoEnlaceDeCuenta(
          EventoEnlaceDeCuenta.RECUPERAR_CLAVE,
          "cuenta-1",
          "ana@example.com",
          "http://kc/enlace",
          30,
          "es",
          Instant.parse("2026-11-12T02:07:56Z"));

  private static ConfiguracionAmqp config(String clave, int puerto) {
    return new ConfiguracionAmqp(
        "127.0.0.1", puerto, "/", "donatrack", clave, ConfiguracionAmqp.EXCHANGE_POR_DEFECTO, 1500);
  }

  @Test
  void sinClaveNoIntentaConectarse() {
    PublicacionFallida error =
        assertThrows(
            PublicacionFallida.class,
            () -> new PublicadorRabbitMq(config(null, 5672)).publicar(EVENTO, "t"));

    assertTrue(error.getMessage().contains("clave"), error.getMessage());
  }

  @Test
  void conUnaClaveEnBlancoTampoco() {
    assertThrows(
        PublicacionFallida.class,
        () -> new PublicadorRabbitMq(config("  ", 5672)).publicar(EVENTO, "t"));
  }

  @Test
  void conElBrokerInalcanzableFallaRapidoYConLaCausa() {
    long inicio = System.nanoTime();

    PublicacionFallida error =
        assertThrows(
            PublicacionFallida.class,
            () -> new PublicadorRabbitMq(config("clave", 1)).publicar(EVENTO, "t"));

    assertInstanceOf(IOException.class, error.getCause());
    assertTrue((System.nanoTime() - inicio) / 1_000_000 < 5_000, "tiene que respetar el timeout");
  }

  @Test
  void elMensajeDeErrorNoLlevaElEmailNiElEnlace() {
    PublicacionFallida error =
        assertThrows(
            PublicacionFallida.class,
            () -> new PublicadorRabbitMq(config("clave", 1)).publicar(EVENTO, "t"));

    assertTrue(!error.getMessage().contains("ana@example.com"), error.getMessage());
    assertTrue(!error.getMessage().contains("http://kc/enlace"), error.getMessage());
  }
}
