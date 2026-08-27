package grupo5.incentivos.infrastructure.adapters;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import grupo5.incentivos.infrastructure.clients.NotificacionesFeignClient;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificacionesClientAdapterTest {

  private NotificacionesClientAdapter adapter;

  @Mock private NotificacionesFeignClient feignClient;

  @BeforeEach
  void setUp() {
    adapter = new NotificacionesClientAdapter(feignClient);
  }

  @Test
  void notificarMisionCumplida_deberiaDelegarAFeign() {
    UUID personaId = UUID.randomUUID();

    adapter.notificarMisionCumplida(personaId, "Mision 1", "Insignia Oro");

    verify(feignClient, times(1)).procesarEvento(any());
  }

  @Test
  void notificarAscensoCategoria_deberiaDelegarAFeign() {
    UUID personaId = UUID.randomUUID();

    adapter.notificarAscensoCategoria(personaId, "SOSTENEDOR", "COLABORADOR");

    verify(feignClient, times(1)).procesarEvento(any());
  }

  @Test
  void notificarInactividad_deberiaDelegarAFeign() {
    UUID personaId = UUID.randomUUID();

    adapter.notificarInactividad(personaId, 45);

    verify(feignClient, times(1)).procesarEvento(any());
  }

  @Test
  void enviar_cuandoFeignFalla_deberiaCapturarExcepcionYNoPropagarla() {
    UUID personaId = UUID.randomUUID();
    doThrow(new RuntimeException("Connection refused")).when(feignClient).procesarEvento(any());

    assertDoesNotThrow(() -> adapter.notificarMisionCumplida(personaId, "Mision 1", "Recompensa"));
  }
}
