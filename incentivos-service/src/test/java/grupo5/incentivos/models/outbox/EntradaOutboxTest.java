package grupo5.incentivos.models.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Month;
import org.junit.jupiter.api.Test;

class EntradaOutboxTest {

  private static final LocalDateTime AHORA = LocalDateTime.of(2026, Month.OCTOBER, 27, 12, 0, 0);

  @Test
  void nueva_empiezaPendienteYSinIntentos() {
    EntradaOutbox entrada = EntradaOutbox.nueva("incentivo.mision-cumplida.v1", "{}", AHORA, 5);

    assertEquals(EstadoOutbox.PENDIENTE, entrada.estado());
    assertEquals(0, entrada.intentos());
    assertEquals(AHORA, entrada.proximoIntento());
  }

  @Test
  void publicada_quedaPublicadaYCuentaElIntento() {
    EntradaOutbox publicada = EntradaOutbox.nueva("rk", "{}", AHORA, 5).publicada();

    assertEquals(EstadoOutbox.PUBLICADO, publicada.estado());
    assertEquals(1, publicada.intentos());
  }

  @Test
  void fallida_reprogramaConEsperaCreciente() {
    EntradaOutbox entrada = EntradaOutbox.nueva("rk", "{}", AHORA, 10);

    EntradaOutbox primera = entrada.fallida(AHORA);
    EntradaOutbox segunda = primera.fallida(AHORA);
    EntradaOutbox tercera = segunda.fallida(AHORA);

    assertEquals(EstadoOutbox.PENDIENTE, primera.estado());
    assertEquals(AHORA.plus(Duration.ofSeconds(5)), primera.proximoIntento());
    assertEquals(AHORA.plus(Duration.ofSeconds(10)), segunda.proximoIntento());
    assertEquals(AHORA.plus(Duration.ofSeconds(20)), tercera.proximoIntento());
    assertEquals(3, tercera.intentos());
  }

  @Test
  void fallida_laEsperaTieneTope() {
    EntradaOutbox entrada = EntradaOutbox.nueva("rk", "{}", AHORA, 100);
    for (int i = 0; i < 30; i++) {
      entrada = entrada.fallida(AHORA);
    }

    assertEquals(AHORA.plus(Duration.ofMinutes(5)), entrada.proximoIntento());
  }

  @Test
  void fallida_alAgotarLosIntentos_quedaFallida() {
    EntradaOutbox entrada = EntradaOutbox.nueva("rk", "{}", AHORA, 2);

    EntradaOutbox primera = entrada.fallida(AHORA);
    EntradaOutbox segunda = primera.fallida(AHORA);

    assertEquals(EstadoOutbox.PENDIENTE, primera.estado());
    assertEquals(EstadoOutbox.FALLIDO, segunda.estado());
    assertEquals(2, segunda.intentos());
  }
}
