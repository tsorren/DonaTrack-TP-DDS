package grupo5.donaciones.infrastructure.persistencia.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import grupo5.donaciones.dto.logistica.DatosEntregaLogistica;
import grupo5.donaciones.infrastructure.persistencia.entities.logistica.OutboxLogisticaEntity;
import grupo5.donaciones.services.logistica.EntradaOutboxLogistica;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** El payload del outbox suma {@code donanteId} sin romper las entradas guardadas antes (HB-16). */
class LogisticaPersistenciaMapperOutboxTest {

  private final LogisticaPersistenciaMapper mapper = new LogisticaPersistenciaMapper();

  @Test
  void toDomain_payloadViejoSinDonante_seLeeConElDonanteEnNull() {
    OutboxLogisticaEntity entity =
        entidadCon(mapper.toEntity(entrada(null)).getPayload().replace(",\"donanteId\":null", ""));

    assertFalse(entity.getPayload().contains("donanteId"));

    DatosEntregaLogistica datos = mapper.toDomain(entity).getDatos();

    assertNull(datos.donanteId());
    assertEquals(10.0, datos.pesoTotalKG());
  }

  @Test
  void idaYVuelta_conservaElDonante() {
    UUID donanteId = UUID.randomUUID();
    OutboxLogisticaEntity entity = mapper.toEntity(entrada(donanteId));

    assertEquals(
        donanteId, mapper.toDomain(entidadCon(entity.getPayload())).getDatos().donanteId());
  }

  private static EntradaOutboxLogistica entrada(UUID donanteId) {
    return EntradaOutboxLogistica.nueva(
        UUID.randomUUID(),
        "donatrack",
        new DatosEntregaLogistica(
            UUID.randomUUID(),
            UUID.randomUUID(),
            null,
            10.0,
            1.0,
            LocalDateTime.of(2026, 11, 8, 12, 0),
            donanteId),
        "trace-1",
        3,
        LocalDateTime.of(2026, 11, 8, 12, 0));
  }

  private OutboxLogisticaEntity entidadCon(String payload) {
    OutboxLogisticaEntity entity = mapper.toEntity(entrada(null));
    entity.setPayload(payload);
    return entity;
  }
}
