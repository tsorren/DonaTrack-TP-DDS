package grupo5.donaciones.services.logistica;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EntradaOutboxLogisticaTest {

  private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 7, 12, 0);

  private static EntradaOutboxLogistica nueva(int maxIntentos) {
    return EntradaOutboxLogistica.nueva(
        UUID.randomUUID(), "donatrack", null, "trace", maxIntentos, AHORA);
  }

  @Test
  void nueva_deberiaEstarPendienteYListaEnElMomento() {
    EntradaOutboxLogistica entrada = nueva(5);

    assertEquals(EstadoEntradaOutbox.PENDIENTE, entrada.getEstado());
    assertEquals(0, entrada.getIntentos());
    assertTrue(entrada.estaListaPara(AHORA));
  }

  @Test
  void registrarIntentoIncierto_deberiaReprogramarConBackoffExponencial() {
    EntradaOutboxLogistica entrada = nueva(5);

    entrada.registrarIntentoIncierto(AHORA, 30);
    assertEquals(AHORA.plusSeconds(60), entrada.getProximoIntento());

    entrada.registrarIntentoIncierto(AHORA, 30);
    assertEquals(AHORA.plusSeconds(120), entrada.getProximoIntento());

    assertEquals(2, entrada.getIntentos());
    assertTrue(entrada.estaPendiente());
    assertFalse(entrada.estaListaPara(AHORA.plusSeconds(119)));
    assertTrue(entrada.estaListaPara(AHORA.plusSeconds(120)));
  }

  @Test
  void registrarIntentoIncierto_deberiaFallar_CuandoSeAgotanLosIntentos() {
    EntradaOutboxLogistica entrada = nueva(2);

    entrada.registrarIntentoIncierto(AHORA, 30);
    entrada.registrarIntentoIncierto(AHORA, 30);

    assertEquals(EstadoEntradaOutbox.FALLIDO, entrada.getEstado());
    assertFalse(entrada.estaListaPara(AHORA.plusDays(1)));
  }

  @Test
  void entradasResueltas_noDeberianEstarListas() {
    EntradaOutboxLogistica publicada = nueva(5);
    publicada.marcarPublicada();
    EntradaOutboxLogistica fallida = nueva(5);
    fallida.marcarFallida();

    assertFalse(publicada.estaListaPara(AHORA));
    assertFalse(fallida.estaListaPara(AHORA));
  }
}
