package grupo5.donaciones.infrastructure.outbox;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OutboxStoreTest {

  private OutboxStore store;

  @BeforeEach
  void setUp() {
    store = new OutboxStore();
  }

  @Test
  void agregar_entradaDisponibleInmediatamente() {
    OutboxEntry entry = OutboxEntry.nuevo("test.llamada", () -> {});

    store.agregar(entry);

    List<OutboxEntry> listos = store.obtenerListosParaReintentar();
    assertEquals(1, listos.size());
    assertEquals(entry.getId(), listos.get(0).getId());
  }

  @Test
  void remover_eliminaEntradaDelStore() {
    OutboxEntry entry = OutboxEntry.nuevo("test.llamada", () -> {});
    store.agregar(entry);

    store.remover(entry.getId());

    assertTrue(store.obtenerListosParaReintentar().isEmpty());
  }

  @Test
  void registrarFallo_primerIntento_encolarConBackoffYNoDisponibleDeInmediato() {
    OutboxEntry entry = OutboxEntry.nuevo("test.llamada", () -> {});
    store.agregar(entry);

    store.registrarFallo(entry);

    assertEquals(1, entry.getIntentos());
    assertTrue(store.obtenerListosParaReintentar().isEmpty());
  }

  @Test
  void registrarFallo_alAgotarMaxIntentos_descartaEntrada() {
    OutboxEntry entry = OutboxEntry.nuevo("test.llamada", () -> {});
    store.agregar(entry);

    for (int i = 0; i < 5; i++) {
      store.registrarFallo(entry);
    }

    assertTrue(entry.agotoIntentos());
    assertTrue(store.obtenerListosParaReintentar().isEmpty());
  }

  @Test
  void registrarFallo_antesDeAgotar_mantieneEntradaEnStore() {
    OutboxEntry entry = OutboxEntry.nuevo("test.llamada", () -> {});
    store.agregar(entry);

    store.registrarFallo(entry);
    store.registrarFallo(entry);

    assertFalse(entry.agotoIntentos());
    assertEquals(2, entry.getIntentos());
  }
}
