package grupo5.donaciones;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import grupo5.donaciones.dto.comunicaciones.EventoPersonaSincronizadaV1;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import grupo5.donaciones.services.INotificacionesAsyncService;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DonacionesServiceApplicationTest {

  @Autowired private INotificacionesAsyncService notificacionesAsyncService;

  @Test
  void contextLoads() {}

  @Test
  void asyncMethod_deberiaEjecutarseSinLanzarAsyncConfigurerException() {
    EventoPersonaSincronizadaV1 evento =
        new EventoPersonaSincronizadaV1(
            UUID.randomUUID(),
            "Test Organismo",
            TipoPersona.HUMANA.name(),
            Collections.emptyList());
    assertDoesNotThrow(() -> notificacionesAsyncService.sincronizarPersona(evento));
  }
}
