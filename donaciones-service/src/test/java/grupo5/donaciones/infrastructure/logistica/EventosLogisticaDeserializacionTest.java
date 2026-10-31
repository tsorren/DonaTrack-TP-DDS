package grupo5.donaciones.infrastructure.logistica;

import static org.junit.jupiter.api.Assertions.assertEquals;

import grupo5.donaciones.dto.comunicaciones.EventoEntregaExitosa;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaFallida;
import grupo5.donaciones.dto.comunicaciones.EventoRutaAsignada;
import grupo5.donaciones.dto.comunicaciones.EventoRutaIniciada;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

/**
 * Donaciones lee las fechas de los eventos de Logística con sufijo Z (RFC 3339, como exigen los
 * schemas) y sin él (mensajes publicados antes del cambio). Mismo mapper que el conversor AMQP.
 */
class EventosLogisticaDeserializacionTest {

  private static final LocalDateTime FECHA = LocalDateTime.of(2026, Month.OCTOBER, 22, 14, 30, 5);
  private static final String ID = UUID.randomUUID().toString();

  private final JsonMapper mapper = JsonMapper.builder().build();

  @ParameterizedTest
  @ValueSource(strings = {"2026-10-22T14:30:05Z", "2026-10-22T14:30:05"})
  void leeLasFechasConYSinSufijoZ(String fecha) {
    assertEquals(
        FECHA,
        mapper
            .readValue(
                """
                {"rutaId":"%s","donacionIndependienteId":"%s","fechaAsignacion":"%s"}
                """
                    .formatted(ID, ID, fecha),
                EventoRutaAsignada.class)
            .fechaAsignacion());
    assertEquals(
        FECHA,
        mapper
            .readValue(
                """
                {"rutaId":"%s","camionId":"%s","patenteCamion":"AB123CD",
                 "donacionesIndependientesIds":["%s"],"fechaInicio":"%s","urlMapa":"https://mapa"}
                """
                    .formatted(ID, ID, ID, fecha),
                EventoRutaIniciada.class)
            .fechaInicio());
    assertEquals(
        FECHA,
        mapper
            .readValue(
                """
                {"entregaId":"%s","donacionIndependienteId":"%s","camionId":"%s",
                 "patenteCamion":"AB123CD","fechaEntrega":"%s"}
                """
                    .formatted(ID, ID, ID, fecha),
                EventoEntregaExitosa.class)
            .fechaEntrega());
    assertEquals(
        FECHA,
        mapper
            .readValue(
                """
                {"entregaId":"%s","donacionIndependienteId":"%s","justificacion":"Cerrado",
                 "fechaFalla":"%s","replanificable":true}
                """
                    .formatted(ID, ID, fecha),
                EventoEntregaFallida.class)
            .fechaFalla());
  }
}
