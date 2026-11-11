package grupo5.notificaciones.infrastructure.persistencia.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import grupo5.notificaciones.infrastructure.persistencia.entities.NotificacionEntity;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificacionPersistenciaMapperTest {

  private final NotificacionPersistenciaMapper mapper = new NotificacionPersistenciaMapper();

  @Test
  void ida_y_vuelta_conservaTipoEventoYReferencia() {
    UUID donacionId = UUID.randomUUID();
    Notificacion original = new Notificacion(UUID.randomUUID(), "Tu donación salió");
    original.etiquetar("DONACION_EN_CAMINO", donacionId);

    NotificacionEntity entity = mapper.toEntity(original);
    Notificacion vuelta = mapper.toDomain(entity);

    assertEquals("DONACION_EN_CAMINO", entity.getTipoEvento());
    assertEquals(donacionId, entity.getReferenciaId());
    assertEquals("DONACION_EN_CAMINO", vuelta.getTipoEvento());
    assertEquals(donacionId, vuelta.getReferenciaId());
  }

  @Test
  void marcarComoLeida_noPierdeTipoNiReferencia() {
    UUID donacionId = UUID.randomUUID();
    Notificacion original = new Notificacion(UUID.randomUUID(), "Llegó");
    original.etiquetar("DONACION_RECIBIDA", donacionId);
    Notificacion releida = mapper.toDomain(mapper.toEntity(original));

    releida.marcarComoLeida();
    Notificacion guardada = mapper.toDomain(mapper.toEntity(releida));

    assertEquals("DONACION_RECIBIDA", guardada.getTipoEvento());
    assertEquals(donacionId, guardada.getReferenciaId());
  }

  @Test
  void notificacionAnterior_sinTipoNiReferencia_quedaEnNulo() {
    NotificacionEntity vieja = mapper.toEntity(new Notificacion(UUID.randomUUID(), "Vieja"));

    Notificacion dominio = mapper.toDomain(vieja);

    assertNull(dominio.getTipoEvento());
    assertNull(dominio.getReferenciaId());
  }
}
