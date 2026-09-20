package grupo5.donaciones.services;

import static org.mockito.Mockito.*;

import grupo5.donaciones.dto.comunicaciones.EventoPersonaSincronizadaV1;
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
    EventoPersonaSincronizadaV1 evento =
        new EventoPersonaSincronizadaV1(UUID.randomUUID(), "Juan Perez", "HUMANA", List.of());

    service.sincronizarPersona(evento);

    verify(eventPublisher, times(1)).publicarPersonaSincronizada(evento);
  }

  @Test
  void sincronizarPersona_deberiaCapturarExcepcionYNoPropagarla_CuandoPublisherFalla() {
    EventoPersonaSincronizadaV1 evento =
        new EventoPersonaSincronizadaV1(UUID.randomUUID(), "Juan Perez", "HUMANA", List.of());
    doThrow(new RuntimeException("Error de conexión"))
        .when(eventPublisher)
        .publicarPersonaSincronizada(any());

    // No debe lanzar excepción
    service.sincronizarPersona(evento);

    verify(eventPublisher, times(1)).publicarPersonaSincronizada(evento);
  }

  @Test
  void sincronizarPersona_noDebeLlamarAlPublisher_CuandoEventoEsNulo() {
    service.sincronizarPersona(null);

    verify(eventPublisher, never()).publicarPersonaSincronizada(any());
  }
}
