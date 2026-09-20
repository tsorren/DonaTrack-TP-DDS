package grupo5.logistica.services.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import grupo5.logistica.testutils.SolicitudPlanificacionMother;
import org.junit.jupiter.api.Test;

class SolicitudPlanificacionMapperTest {
  private final SolicitudPlanificacionMapper mapper = new SolicitudPlanificacionMapper();

  @Test
  void mapeaTodosLosCampos() {
    SolicitudPlanificacion solicitud = SolicitudPlanificacionMother.enError();
    var dto = mapper.toResponseDTO(solicitud);
    assertEquals(solicitud.getId(), dto.id());
    assertEquals(solicitud.getEstado(), dto.estado());
    assertEquals(solicitud.getIntentosFallidos(), dto.intentosFallidos());
    assertEquals(solicitud.getMotivoError(), dto.motivoError());
  }

  @Test
  void aceptaNulo() {
    assertNull(mapper.toResponseDTO(null));
  }
}
