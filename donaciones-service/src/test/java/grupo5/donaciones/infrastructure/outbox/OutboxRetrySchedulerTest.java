package grupo5.donaciones.infrastructure.outbox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxRetrySchedulerTest {

  @Mock private OutboxStore outboxStore;

  @InjectMocks private OutboxRetryScheduler scheduler;

  @Test
  void procesarPendientes_cuandoNoHayEntradas_noInteractuaConElStore() {
    when(outboxStore.obtenerListosParaReintentar()).thenReturn(Collections.emptyList());

    scheduler.procesarPendientes();

    verify(outboxStore, never()).remover(any());
    verify(outboxStore, never()).registrarFallo(any());
  }

  @Test
  void procesarPendientes_cuandoAccionExitosa_removeEntradaDelStore() {
    OutboxEntry entry = OutboxEntry.nuevo("test.llamada", () -> {});
    when(outboxStore.obtenerListosParaReintentar()).thenReturn(List.of(entry));

    scheduler.procesarPendientes();

    verify(outboxStore).remover(entry.getId());
    verify(outboxStore, never()).registrarFallo(any());
  }

  @Test
  void procesarPendientes_cuandoAccionFalla_registraFalloSinRemover() {
    OutboxEntry entry =
        OutboxEntry.nuevo(
            "test.llamada",
            () -> {
              throw new RuntimeException("servicio caído");
            });
    when(outboxStore.obtenerListosParaReintentar()).thenReturn(List.of(entry));

    scheduler.procesarPendientes();

    verify(outboxStore).registrarFallo(entry);
    verify(outboxStore, never()).remover(any());
  }

  @Test
  void procesarPendientes_cuandoVariasEntradas_procesaTodasIndependientemente() {
    OutboxEntry exitosa = OutboxEntry.nuevo("test.exitosa", () -> {});
    OutboxEntry fallida =
        OutboxEntry.nuevo(
            "test.fallida",
            () -> {
              throw new RuntimeException();
            });
    when(outboxStore.obtenerListosParaReintentar()).thenReturn(List.of(exitosa, fallida));

    scheduler.procesarPendientes();

    verify(outboxStore).remover(exitosa.getId());
    verify(outboxStore).registrarFallo(fallida);
  }
}
