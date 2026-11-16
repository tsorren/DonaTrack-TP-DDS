package grupo5.logistica.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import grupo5.logistica.dto.entregas.ReporteRecepcionDTO;
import grupo5.logistica.dto.eventos.EventoEntregaExitosa;
import grupo5.logistica.models.entities.entregas.MotivoReporte;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** HB-17: entrega.exitosa lleva el reporte solo si lo hubo, y nunca el remito. */
class EventoEntregaExitosaSerializacionTest {

  // Mismo mapper que el conversor AMQP (RabbitMQConfig)
  private final JsonMapper mapper = JsonMapper.builder().build();

  private EventoEntregaExitosa evento(ReporteRecepcionDTO reporte) {
    return evento(reporte, List.of());
  }

  private EventoEntregaExitosa evento(ReporteRecepcionDTO reporte, List<String> fotos) {
    return new EventoEntregaExitosa(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "AB123CD",
        LocalDateTime.of(2026, 11, 8, 10, 0),
        reporte,
        fotos);
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

  @Test
  void sinFotosNoPublicaElCampo() {
    JsonNode json = mapper.readTree(mapper.writeValueAsString(evento(null)));

    assertFalse(json.has("fotosRecepcion"));
  }

  @Test
  void conFotosLasPublicaEnOrden() {
    JsonNode json =
        mapper.readTree(
            mapper.writeValueAsString(
                evento(
                    null,
                    List.of(
                        "http://localhost:8086/api/entregas/1/fotos/a",
                        "https://ejemplo.org/b.jpg"))));

    assertEquals(2, json.get("fotosRecepcion").size());
    assertEquals(
        "http://localhost:8086/api/entregas/1/fotos/a",
        json.get("fotosRecepcion").get(0).asString());
    assertEquals("https://ejemplo.org/b.jpg", json.get("fotosRecepcion").get(1).asString());
  }
}
