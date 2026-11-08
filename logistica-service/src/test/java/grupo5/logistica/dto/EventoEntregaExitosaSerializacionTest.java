package grupo5.logistica.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import grupo5.logistica.dto.entregas.ReporteRecepcionDTO;
import grupo5.logistica.dto.eventos.EventoEntregaExitosa;
import grupo5.logistica.models.entities.entregas.MotivoReporte;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** HB-17: entrega.exitosa lleva el reporte solo si lo hubo, y nunca el remito. */
class EventoEntregaExitosaSerializacionTest {

  // Mismo mapper que el conversor AMQP (RabbitMQConfig)
  private final JsonMapper mapper = JsonMapper.builder().build();

  private EventoEntregaExitosa evento(ReporteRecepcionDTO reporte) {
    return new EventoEntregaExitosa(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "AB123CD",
        LocalDateTime.of(2026, 11, 8, 10, 0),
        reporte);
  }

  @Test
  void sinReporteNoPublicaElCampo() {
    JsonNode json = mapper.readTree(mapper.writeValueAsString(evento(null)));

    assertFalse(json.has("reporte"));
    assertFalse(json.has("numeroRemito"));
  }

  @Test
  void conReportePublicaMotivoDetalleYCantidad() {
    JsonNode json =
        mapper.readTree(
            mapper.writeValueAsString(
                evento(new ReporteRecepcionDTO(MotivoReporte.INCOMPLETA, "Faltó una", 12.5))));

    assertEquals("INCOMPLETA", json.get("reporte").get("motivo").asString());
    assertEquals(12.5, json.get("reporte").get("cantidadRecibida").asDouble());
    assertFalse(json.has("numeroRemito"));
  }
}
