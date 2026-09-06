package grupo5.donaciones;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

import grupo5.donaciones.dto.comunicaciones.PersonaReplicaDTO;
import grupo5.donaciones.infrastructure.clients.IncentivosFeignClient;
import grupo5.donaciones.infrastructure.clients.LogisticaFeignClient;
import grupo5.donaciones.infrastructure.clients.NotificacionesFeignClient;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import grupo5.donaciones.services.INotificacionesAsyncService;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class DonacionesServiceApplicationTest {

  @MockitoBean private NotificacionesFeignClient notificacionesFeignClient;
  @MockitoBean private IncentivosFeignClient incentivosFeignClient;
  @MockitoBean private LogisticaFeignClient logisticaFeignClient;

  @Autowired private INotificacionesAsyncService notificacionesAsyncService;

  @Test
  void contextLoads() {}

  @Test
  void asyncMethod_deberiaEjecutarseSinLanzarAsyncConfigurerException() {
    doNothing().when(notificacionesFeignClient).sincronizarPersona(any());

    PersonaReplicaDTO dto =
        new PersonaReplicaDTO(
            UUID.randomUUID(), "Test Organismo", TipoPersona.HUMANA, Collections.emptyList());
    assertDoesNotThrow(() -> notificacionesAsyncService.sincronizarPersona(dto));
  }
}
