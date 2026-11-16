package grupo5.notificaciones.dto.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * HB-09: el id de la donación es opcional en los eventos; un emisor que no lo manda sigue valiendo.
 */
class EventoV1ReferenciaJsonTest {

  private static final String BASE =
      """
      "donanteId": "6f1c2c1e-0000-4000-8000-000000000001",
      "personaId": "6f1c2c1e-0000-4000-8000-000000000001",
      "fecha": "2026-11-10T12:00:00Z",
      "personaBeneficiariaId": "6f1c2c1e-0000-4000-8000-000000000002",
      "descripcion": "Arroz",
      "patenteCamion": "AB123CD"
      """;

  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @Test
  void sinDonacionIndependienteId_quedaEnNulo() throws Exception {
    EventoDonacionRecibidaV1 evento =
        mapper.readValue("{" + BASE + "}", EventoDonacionRecibidaV1.class);

    assertNull(evento.donacionIndependienteId());
  }

  @Test
  void conDonacionIndependienteId_loLee() throws Exception {
    UUID id = UUID.randomUUID();

    EventoDonacionRecibidaV1 evento =
        mapper.readValue(
            "{" + BASE + ", \"donacionIndependienteId\": \"" + id + "\"}",
            EventoDonacionRecibidaV1.class);

    assertEquals(id, evento.donacionIndependienteId());
  }
}
