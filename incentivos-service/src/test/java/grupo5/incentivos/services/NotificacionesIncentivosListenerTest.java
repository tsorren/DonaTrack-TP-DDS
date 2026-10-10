package grupo5.incentivos.services;

import static org.mockito.Mockito.*;

import grupo5.incentivos.infrastructure.IN8nClient;
import grupo5.incentivos.infrastructure.INotificacionesClient;
import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.eventos.AscensoDonante;
import grupo5.incentivos.models.entities.donante.eventos.MisionCompletada;
import grupo5.incentivos.models.entities.insignias.Insignia;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificacionesIncentivosListenerTest {

  private NotificacionesIncentivosListener listener;

  @Mock private INotificacionesClient notificacionesClient;
  @Mock private IN8nClient n8nClient;

  @BeforeEach
  void setUp() {
    listener = new NotificacionesIncentivosListener(notificacionesClient, n8nClient);
  }

  @Test
  void onMisionCompletada_conInsignia_deberiaNotificarPorClienteyN8n() {
    UUID donanteId = UUID.randomUUID();
    UUID personaId = UUID.randomUUID();
    Insignia insignia = new Insignia("Gran Donador", "Por donar mucho", "/icon.png");
    MisionCompletada evento =
        new MisionCompletada(donanteId, personaId, "Carlos", "Misión 1", insignia);

    listener.onMisionCompletada(evento);

    verify(notificacionesClient, times(1))
        .notificarMisionCumplida(personaId, "Misión 1", "Gran Donador");
    verify(n8nClient, times(1))
        .publicarInsigniaGanada(donanteId, "Donante Carlos", "Gran Donador", "Por donar mucho");
  }

  @Test
  void onMisionCompletada_sinInsignia_deberiaNotificarSoloPorCliente() {
    UUID donanteId = UUID.randomUUID();
    UUID personaId = UUID.randomUUID();
    MisionCompletada evento =
        new MisionCompletada(donanteId, personaId, "Carlos", "Misión Simple", null);

    listener.onMisionCompletada(evento);

    verify(notificacionesClient, times(1))
        .notificarMisionCumplida(personaId, "Misión Simple", "Sin recompensa");
    verify(n8nClient, never()).publicarInsigniaGanada(any(), any(), any(), any());
  }

  @Test
  void onAscensoDonante_deberiaNotificarAscensoDeCategoria() {
    UUID donanteId = UUID.randomUUID();
    UUID personaId = UUID.randomUUID();
    AscensoDonante evento =
        new AscensoDonante(
            donanteId, personaId, CategoriaDonante.COLABORADOR, CategoriaDonante.SOSTENEDOR);

    listener.onAscensoDonante(evento);

    verify(notificacionesClient, times(1))
        .notificarAscensoCategoria(
            personaId, CategoriaDonante.SOSTENEDOR.name(), CategoriaDonante.COLABORADOR.name());
  }
}
