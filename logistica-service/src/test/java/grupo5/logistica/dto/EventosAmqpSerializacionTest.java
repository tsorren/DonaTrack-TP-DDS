package grupo5.logistica.dto;

import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.logistica.dto.eventos.EventoEntregaExitosa;
import grupo5.logistica.dto.eventos.EventoEntregaFallida;
import grupo5.logistica.dto.eventos.EventoRutaAsignada;
import grupo5.logistica.dto.eventos.EventoRutaIniciada;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Las fechas de los eventos que publica Logística salen en RFC 3339 con sufijo Z, como exigen sus
 * schemas {@code evento-*} (mismo mapper que el conversor de {@code RabbitMQConfig}).
 */
class EventosAmqpSerializacionTest {

  private static final LocalDateTime FECHA = LocalDateTime.of(2026, 10, 22, 14, 30, 5, 123456789);
  private static final String FECHA_RFC3339 = "\"2026-10-22T14:30:05Z\"";

  private final JsonMapper mapper = JsonMapper.builder().build();

  @Test
  void lasFechasDeLosEventosSalenEnRfc3339ConSufijoZ() {
    UUID id = UUID.randomUUID();
    List<Object> eventos =
        List.of(
            new EventoRutaAsignada(id, id, FECHA),
            new EventoRutaIniciada(id, id, "AB123CD", List.of(id), FECHA, "https://mapa"),
            new EventoEntregaExitosa(id, id, id, "AB123CD", FECHA),
            new EventoEntregaFallida(id, id, "Nadie atendió", FECHA, true));

    for (Object evento : eventos) {
      String json = mapper.writeValueAsString(evento);
      assertTrue(json.contains(FECHA_RFC3339), evento.getClass().getSimpleName() + ": " + json);
    }
  }
}
