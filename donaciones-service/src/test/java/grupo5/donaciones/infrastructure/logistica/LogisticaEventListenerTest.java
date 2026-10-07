package grupo5.donaciones.infrastructure.logistica;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import grupo5.donaciones.config.RabbitMQConfig;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaExitosa;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaFallida;
import grupo5.donaciones.dto.comunicaciones.EventoRutaAsignada;
import grupo5.donaciones.dto.comunicaciones.EventoRutaIniciada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** El listener AMQP solo delega en el procesador, indicando de qué cola vino cada evento. */
class LogisticaEventListenerTest {

  private ProcesadorEventosLogistica procesador;
  private LogisticaEventListener listener;

  @BeforeEach
  void setUp() {
    procesador = mock(ProcesadorEventosLogistica.class);
    listener = new LogisticaEventListener(procesador);
  }

  @Test
  void onRutaAsignada_delegaEnElProcesadorConSuCola() {
    EventoRutaAsignada evento = mock(EventoRutaAsignada.class);

    listener.onRutaAsignada(evento);

    verify(procesador).procesarRutaAsignada(evento, RabbitMQConfig.QUEUE_RUTA_ASIGNADA);
  }

  @Test
  void onRutaIniciada_delegaEnElProcesadorConSuCola() {
    EventoRutaIniciada evento = mock(EventoRutaIniciada.class);

    listener.onRutaIniciada(evento);

    verify(procesador).procesarRutaIniciada(evento, RabbitMQConfig.QUEUE_RUTA_INICIADA);
  }

  @Test
  void onEntregaExitosa_delegaEnElProcesadorConSuCola() {
    EventoEntregaExitosa evento = mock(EventoEntregaExitosa.class);

    listener.onEntregaExitosa(evento);

    verify(procesador).procesarEntregaExitosa(evento, RabbitMQConfig.QUEUE_ENTREGA_EXITOSA);
  }

  @Test
  void onEntregaFallida_delegaEnElProcesadorConSuCola() {
    EventoEntregaFallida evento = mock(EventoEntregaFallida.class);

    listener.onEntregaFallida(evento);

    verify(procesador).procesarEntregaFallida(evento, RabbitMQConfig.QUEUE_ENTREGA_FALLIDA);
  }
}
