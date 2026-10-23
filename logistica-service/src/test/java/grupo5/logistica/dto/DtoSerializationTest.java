package grupo5.logistica.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import grupo5.logistica.dto.entregas.CambioEstadoEntregaResponseDTO;
import grupo5.logistica.dto.entregas.EntregaMapaPublicoResponseDTO;
import grupo5.logistica.dto.entregas.EntregaResponseDTO;
import grupo5.logistica.dto.entregas.ParadaSeguimientoDTO;
import grupo5.logistica.dto.entregas.SeguimientoEntregaResponseDTO;
import grupo5.logistica.dto.rutas.DireccionDTO;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DtoSerializationTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  @Test
  @DisplayName(
      "EntregaMapaPublicoResponseDTO serializa fechaEntrega en UTC ISO-8601 con sufijo Z y es bidireccional")
  void entregaMapaPublicoResponse_serializaFechaUtcIso8601() throws Exception {
    Instant fecha = Instant.parse("2026-10-22T14:30:00Z");
    EntregaMapaPublicoResponseDTO dto =
        new EntregaMapaPublicoResponseDTO(
            UUID.randomUUID(),
            -34.6037,
            -58.3816,
            "CABA",
            "Buenos Aires",
            "Argentina",
            fecha,
            10.5f,
            0.5f);

    String json = objectMapper.writeValueAsString(dto);

    assertTrue(
        json.contains("\"fechaEntrega\":\"2026-10-22T14:30:00Z\""),
        "Debe contener fecha en UTC con Z");

    EntregaMapaPublicoResponseDTO deserializado =
        objectMapper.readValue(json, EntregaMapaPublicoResponseDTO.class);
    assertEquals(fecha, deserializado.fechaEntrega());
  }

  @Test
  @DisplayName("SeguimientoEntregaResponseDTO serializa instantes en UTC ISO-8601 con sufijo Z")
  void seguimientoEntregaResponse_serializaInstantesUtcIso8601() throws Exception {
    Instant salida = Instant.parse("2026-10-22T10:00:00Z");
    Instant arribo = Instant.parse("2026-10-22T12:00:00Z");
    Instant actualizacion = Instant.parse("2026-10-22T11:15:30Z");

    SeguimientoEntregaResponseDTO dto =
        new SeguimientoEntregaResponseDTO(
            UUID.randomUUID(),
            UUID.randomUUID(),
            EstadoEntrega.EN_TRASLADO,
            true,
            UUID.randomUUID(),
            LocalDate.of(2026, 10, 22),
            EstadoRuta.EN_TRASLADO,
            "AB123CD",
            1,
            3,
            1,
            1,
            salida,
            arribo,
            actualizacion,
            List.of());

    String json = objectMapper.writeValueAsString(dto);

    assertTrue(json.contains("\"horaSalida\":\"2026-10-22T10:00:00Z\""));
    assertTrue(json.contains("\"horaArribo\":\"2026-10-22T12:00:00Z\""));
    assertTrue(json.contains("\"ultimaActualizacion\":\"2026-10-22T11:15:30Z\""));

    SeguimientoEntregaResponseDTO deserializado =
        objectMapper.readValue(json, SeguimientoEntregaResponseDTO.class);
    assertEquals(salida, deserializado.horaSalida());
    assertEquals(arribo, deserializado.horaArribo());
    assertEquals(actualizacion, deserializado.ultimaActualizacion());
  }

  @Test
  @DisplayName("ParadaSeguimientoDTO serializa horaArribo en UTC ISO-8601 con sufijo Z")
  void paradaSeguimiento_serializaHoraArriboUtcIso8601() throws Exception {
    Instant arribo = Instant.parse("2026-10-22T11:45:00Z");
    ParadaSeguimientoDTO dto = new ParadaSeguimientoDTO(1, EstadoEntrega.EN_TRASLADO, true, arribo);

    String json = objectMapper.writeValueAsString(dto);

    assertTrue(json.contains("\"horaArribo\":\"2026-10-22T11:45:00Z\""));

    ParadaSeguimientoDTO deserializado = objectMapper.readValue(json, ParadaSeguimientoDTO.class);
    assertEquals(arribo, deserializado.horaArribo());
  }

  @Test
  @DisplayName("EntregaResponseDTO serializa horaSalida y horaArribo en UTC ISO-8601")
  void entregaResponse_serializaInstantesUtcIso8601() throws Exception {
    Instant salida = Instant.parse("2026-10-22T08:00:00Z");
    Instant arribo = Instant.parse("2026-10-22T09:30:00Z");

    EntregaResponseDTO dto =
        new EntregaResponseDTO(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            new DireccionDTO(
                "Calle Falsa",
                123,
                1,
                null,
                "C1000",
                "CABA",
                "Buenos Aires",
                "Argentina",
                -34.6,
                -58.3),
            EstadoEntrega.ENTREGADA,
            salida,
            arribo,
            "https://minio.local/foto1.jpg",
            List.of("https://minio.local/foto1.jpg"),
            10f,
            1f,
            List.of());

    String json = objectMapper.writeValueAsString(dto);

    assertTrue(json.contains("\"horaSalida\":\"2026-10-22T08:00:00Z\""));
    assertTrue(json.contains("\"horaArribo\":\"2026-10-22T09:30:00Z\""));

    EntregaResponseDTO deserializado = objectMapper.readValue(json, EntregaResponseDTO.class);
    assertEquals(salida, deserializado.horaSalida());
    assertEquals(arribo, deserializado.horaArribo());
  }

  @Test
  @DisplayName("CambioEstadoEntregaResponseDTO serializa timestamp en UTC ISO-8601")
  void cambioEstadoEntregaResponse_serializaTimestampUtcIso8601() throws Exception {
    Instant timestamp = Instant.parse("2026-10-22T15:20:10Z");

    CambioEstadoEntregaResponseDTO dto =
        new CambioEstadoEntregaResponseDTO(
            EstadoEntrega.PENDIENTE, EstadoEntrega.EN_TRASLADO, timestamp, "Inicio de ruta");

    String json = objectMapper.writeValueAsString(dto);

    assertTrue(json.contains("\"timestamp\":\"2026-10-22T15:20:10Z\""));

    CambioEstadoEntregaResponseDTO deserializado =
        objectMapper.readValue(json, CambioEstadoEntregaResponseDTO.class);
    assertEquals(timestamp, deserializado.timestamp());
  }
}
