package grupo5.donaciones.infrastructure.logistica;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.donaciones.config.RabbitMQConfig;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaExitosa;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaFallida;
import grupo5.donaciones.dto.comunicaciones.EventoRutaAsignada;
import grupo5.donaciones.dto.comunicaciones.EventoRutaIniciada;
import grupo5.donaciones.services.logistica.IVerificadorOrigenEventos;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * El listener AMQP verifica de qué proveedor viene el evento y, si es válido, delega en el
 * procesador indicando de qué cola vino.
 */
class LogisticaEventListenerTest {

  private static final String PROVEEDOR = "externo";
  private static final String TOKEN = "token-sintetico";

  private ProcesadorEventosLogistica procesador;
  private IVerificadorOrigenEventos verificador;
  private LogisticaEventListener listener;

  @BeforeEach
  void setUp() {
    procesador = mock(ProcesadorEventosLogistica.class);
    verificador = mock(IVerificadorOrigenEventos.class);
    listener = new LogisticaEventListener(procesador, verificador);
  }

  @Test
  void onRutaAsignada_conOrigenValido_delegaEnElProcesadorConSuCola() {
    UUID donacionId = UUID.randomUUID();
    EventoRutaAsignada evento = new EventoRutaAsignada(UUID.randomUUID(), donacionId, null);
    when(verificador.esOrigenValido(PROVEEDOR, TOKEN, List.of(donacionId))).thenReturn(true);

    listener.onRutaAsignada(evento, PROVEEDOR, TOKEN);

    verify(procesador).procesarRutaAsignada(evento, RabbitMQConfig.QUEUE_RUTA_ASIGNADA);
  }

  @Test
  void onRutaIniciada_conOrigenValido_verificaTodasLasDonacionesYDelega() {
    List<UUID> donaciones = List.of(UUID.randomUUID(), UUID.randomUUID());
    EventoRutaIniciada evento =
        new EventoRutaIniciada(UUID.randomUUID(), null, null, donaciones, null, null);
    when(verificador.esOrigenValido(PROVEEDOR, TOKEN, donaciones)).thenReturn(true);

    listener.onRutaIniciada(evento, PROVEEDOR, TOKEN);

    verify(procesador).procesarRutaIniciada(evento, RabbitMQConfig.QUEUE_RUTA_INICIADA);
  }

  @Test
  void onEntregaExitosa_conOrigenValido_delegaEnElProcesadorConSuCola() {
    UUID donacionId = UUID.randomUUID();
    EventoEntregaExitosa evento =
        new EventoEntregaExitosa(UUID.randomUUID(), donacionId, null, null, null);
    when(verificador.esOrigenValido(PROVEEDOR, TOKEN, List.of(donacionId))).thenReturn(true);

    listener.onEntregaExitosa(evento, PROVEEDOR, TOKEN);

    verify(procesador).procesarEntregaExitosa(evento, RabbitMQConfig.QUEUE_ENTREGA_EXITOSA);
  }

  @Test
  void onEntregaFallida_conOrigenValido_delegaEnElProcesadorConSuCola() {
    UUID donacionId = UUID.randomUUID();
    EventoEntregaFallida evento =
        new EventoEntregaFallida(UUID.randomUUID(), donacionId, "sin acceso", null, false);
    when(verificador.esOrigenValido(PROVEEDOR, TOKEN, List.of(donacionId))).thenReturn(true);

    listener.onEntregaFallida(evento, PROVEEDOR, TOKEN);

    verify(procesador).procesarEntregaFallida(evento, RabbitMQConfig.QUEUE_ENTREGA_FALLIDA);
  }

  @Test
  void conOrigenInvalido_noSeProcesaNingunEvento() {
    UUID donacionId = UUID.randomUUID();
    when(verificador.esOrigenValido(any(), any(), any())).thenReturn(false);

    listener.onRutaAsignada(
        new EventoRutaAsignada(UUID.randomUUID(), donacionId, null), null, null);
    listener.onRutaIniciada(
        new EventoRutaIniciada(UUID.randomUUID(), null, null, List.of(donacionId), null, null),
        PROVEEDOR,
        "otro-token");
    listener.onEntregaExitosa(
        new EventoEntregaExitosa(UUID.randomUUID(), donacionId, null, null, null), PROVEEDOR, null);
    listener.onEntregaFallida(
        new EventoEntregaFallida(UUID.randomUUID(), donacionId, "x", null, false), null, TOKEN);

    verify(procesador, never()).procesarRutaAsignada(any(), any());
    verify(procesador, never()).procesarRutaIniciada(any(), any());
    verify(procesador, never()).procesarEntregaExitosa(any(), any());
    verify(procesador, never()).procesarEntregaFallida(any(), any());
  }

  @Test
  void elIdYElTokenRecibidosSePasanTalCualAlVerificador() {
    UUID donacionId = UUID.randomUUID();

    listener.onRutaAsignada(
        new EventoRutaAsignada(UUID.randomUUID(), donacionId, null), PROVEEDOR, TOKEN);

    verify(verificador).esOrigenValido(eq(PROVEEDOR), eq(TOKEN), eq(List.of(donacionId)));
  }
}
