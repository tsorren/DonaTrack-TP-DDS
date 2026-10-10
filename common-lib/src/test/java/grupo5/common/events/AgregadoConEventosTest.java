package grupo5.common.events;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AgregadoConEventosTest {

  private static class EventoPrueba extends EventoDeDominio {
    private final String detalle;

    public EventoPrueba(String detalle) {
      super();
      this.detalle = detalle;
    }

    public String getDetalle() {
      return detalle;
    }
  }

  private static class AgregadoPrueba extends AgregadoConEventos<EventoPrueba> {
    private final UUID id = UUID.randomUUID();

    @Override
    public UUID getId() {
      return id;
    }

    public void ejecutarAccion(String detalle) {
      registrarEvento(new EventoPrueba(detalle));
    }
  }

  private AgregadoPrueba agregado;

  @BeforeEach
  void setUp() {
    agregado = new AgregadoPrueba();
  }

  @Test
  void nuevoAgregado_iniciaSinEventos() {
    assertTrue(agregado.getDomainEvents().isEmpty());
  }

  @Test
  void registrarEvento_agregaEventoALaLista() {
    agregado.ejecutarAccion("alta");

    List<EventoPrueba> eventos = agregado.getDomainEvents();
    assertEquals(1, eventos.size());
    assertEquals("alta", eventos.getFirst().getDetalle());
    assertNotNull(eventos.getFirst().getId());
    assertNotNull(eventos.getFirst().getTimestamp());
  }

  @Test
  void registrarEvento_conEventoNulo_noAgregaNada() {
    agregado.registrarEvento(null);
    assertTrue(agregado.getDomainEvents().isEmpty());
  }

  @Test
  void getDomainEvents_retornaCopiaInmodificable() {
    agregado.ejecutarAccion("evento1");
    List<EventoPrueba> eventos = agregado.getDomainEvents();

    EventoPrueba nuevoEvento = new EventoPrueba("evento2");
    assertThrows(UnsupportedOperationException.class, () -> eventos.add(nuevoEvento));
  }

  @Test
  void clearDomainEvents_limpiaEventosRegistrados() {
    agregado.ejecutarAccion("evento1");
    assertEquals(1, agregado.getDomainEvents().size());

    agregado.clearDomainEvents();
    assertTrue(agregado.getDomainEvents().isEmpty());
  }
}
