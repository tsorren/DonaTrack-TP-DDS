package grupo5.donaciones.services;

import static org.mockito.Mockito.*;

import grupo5.donaciones.dto.comunicaciones.EventoPersonaSincronizadaV1;
import grupo5.donaciones.dto.comunicaciones.PersonaReplicaDTO;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import grupo5.donaciones.services.impl.NotificacionesAsyncService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificacionesAsyncServiceTest {

  @Mock private IDonacionesEventPublisher eventPublisher;

  @InjectMocks private NotificacionesAsyncService service;

  @Test
  void sincronizarPersona_deberiaPublicarEvento_CuandoNoHayErrores() {
    PersonaReplicaDTO dto =
        new PersonaReplicaDTO(UUID.randomUUID(), "Juan Perez", TipoPersona.HUMANA, List.of());

    service.sincronizarPersona(dto);

    verify(eventPublisher, times(1))
        .publicarPersonaSincronizada(any(EventoPersonaSincronizadaV1.class));
  }

  @Test
  void sincronizarPersona_deberiaCapturarExcepcionYNoPropagarla_CuandoPublisherFalla() {
    PersonaReplicaDTO dto =
        new PersonaReplicaDTO(UUID.randomUUID(), "Juan Perez", TipoPersona.HUMANA, List.of());
    doThrow(new RuntimeException("Error de conexión"))
        .when(eventPublisher)
        .publicarPersonaSincronizada(any());

    // No debe lanzar excepción
    service.sincronizarPersona(dto);

    verify(eventPublisher, times(1))
        .publicarPersonaSincronizada(any(EventoPersonaSincronizadaV1.class));
  }
}
