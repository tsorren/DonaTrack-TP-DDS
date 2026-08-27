package grupo5.logistica.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.logistica.dto.eventos.EventoEntregaExitosa;
import grupo5.logistica.dto.eventos.EventoEntregaFallida;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.entregas.eventos.EntregaConfirmada;
import grupo5.logistica.models.entities.entregas.eventos.EntregaFallida;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ComunicadorEventosLogisticaRabbitTest {

  private final LogisticaEventPublisher eventPublisher = mock(LogisticaEventPublisher.class);
  private final GeneradorDeURLSeguimiento generadorDeUrlSeguimiento =
      mock(GeneradorDeURLSeguimiento.class);
  private final ComunicadorEventosLogisticaRabbit comunicador =
      new ComunicadorEventosLogisticaRabbit(eventPublisher, generadorDeUrlSeguimiento);

  @Test
  void mapeaEntregaConfirmadaAPayloadRabbitConElTimestampDelEventoDeDominio() {
    UUID entregaId = UUID.randomUUID();
    UUID donacionId = UUID.randomUUID();
    UUID camionId = UUID.randomUUID();
    EntregaConfirmada evento = new EntregaConfirmada(entregaId, donacionId, UUID.randomUUID());
    Camion camion = mock(Camion.class);
    when(camion.getId()).thenReturn(camionId);
    when(camion.getPatente()).thenReturn("AA123BB");

    comunicador.comunicarEntregaExitosa(evento, camion);

    ArgumentCaptor<EventoEntregaExitosa> captor =
        ArgumentCaptor.forClass(EventoEntregaExitosa.class);
    verify(eventPublisher).publicarEntregaExitosa(captor.capture());
    EventoEntregaExitosa payload = captor.getValue();
    assertEquals(entregaId, payload.entregaId());
    assertEquals(donacionId, payload.donacionIndependienteId());
    assertEquals(camionId, payload.camionId());
    assertEquals("AA123BB", payload.patenteCamion());
    assertEquals(evento.getTimestamp(), payload.fechaEntrega());
  }

  @Test
  void mapeaEntregaFallidaAPayloadRabbitConElTimestampDelEventoDeDominio() {
    UUID entregaId = UUID.randomUUID();
    UUID donacionId = UUID.randomUUID();
    EntregaFallida evento = new EntregaFallida(entregaId, donacionId, "Domicilio cerrado", false);

    comunicador.comunicarEntregaFallida(evento);

    ArgumentCaptor<EventoEntregaFallida> captor =
        ArgumentCaptor.forClass(EventoEntregaFallida.class);
    verify(eventPublisher).publicarEntregaFallida(captor.capture());
    EventoEntregaFallida payload = captor.getValue();
    assertEquals(entregaId, payload.entregaId());
    assertEquals(donacionId, payload.donacionIndependienteId());
    assertEquals("Domicilio cerrado", payload.justificacion());
    assertEquals(evento.getTimestamp(), payload.fechaFalla());
    assertFalse(payload.replanificable());
  }
}
