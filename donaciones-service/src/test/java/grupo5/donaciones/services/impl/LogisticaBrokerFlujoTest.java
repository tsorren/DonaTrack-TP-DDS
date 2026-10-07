package grupo5.donaciones.services.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.donaciones.dto.logistica.DatosEntregaLogistica;
import grupo5.donaciones.infrastructure.outbox.LogisticaOutboxEnMemoria;
import grupo5.donaciones.infrastructure.outbox.LogisticaOutboxRelay;
import grupo5.donaciones.models.entities.logistica.EstadoSolicitudEntrega;
import grupo5.donaciones.models.entities.logistica.SolicitudEntrega;
import grupo5.donaciones.models.repositories.impl.SolicitudesEntregaRepositoryEnMemoria;
import grupo5.donaciones.services.logistica.EnvioInciertoException;
import grupo5.donaciones.services.logistica.EnvioRechazadoException;
import grupo5.donaciones.services.logistica.IProveedorLogistica;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/** Broker + relay + outbox reales, con proveedores de prueba y un reloj que se puede adelantar. */
class LogisticaBrokerFlujoTest {

  private static final LocalDateTime INICIO = LocalDateTime.of(2026, 10, 7, 12, 0);

  private static final class RelojAjustable extends Clock {
    private Instant ahora = INICIO.toInstant(ZoneOffset.UTC);

    void adelantar(Duration duracion) {
      ahora = ahora.plus(duracion);
    }

    @Override
    public ZoneId getZone() {
      return ZoneId.of("UTC");
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return ahora;
    }
  }

  private RelojAjustable reloj;
  private SolicitudesEntregaRepositoryEnMemoria solicitudes;
  private IProveedorLogistica donatrack;
  private IProveedorLogistica externo;
  private LogisticaBroker broker;
  private LogisticaOutboxRelay relay;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    reloj = new RelojAjustable();
    solicitudes = new SolicitudesEntregaRepositoryEnMemoria();
    LogisticaOutboxEnMemoria outbox = new LogisticaOutboxEnMemoria();
    donatrack = mock(IProveedorLogistica.class);
    externo = mock(IProveedorLogistica.class);
    when(donatrack.id()).thenReturn("donatrack");
    when(externo.id()).thenReturn("externo");

    broker =
        new LogisticaBroker(
            solicitudes,
            outbox,
            new SeleccionPorPreferenciaConFallback(List.of("donatrack", "externo"), "donatrack"),
            reloj,
            3,
            30);
    ObjectProvider<IProveedorLogistica> provider = mock(ObjectProvider.class);
    when(provider.orderedStream()).thenReturn(Stream.of(donatrack, externo));
    relay = new LogisticaOutboxRelay(outbox, broker, provider, reloj);
  }

  private static DatosEntregaLogistica pedido() {
    return new DatosEntregaLogistica(UUID.randomUUID(), UUID.randomUUID(), null, 10.0, 1.0, INICIO);
  }

  private SolicitudEntrega unicaSolicitud() {
    assertEquals(1, solicitudes.count());
    return solicitudes.findAll().get(0);
  }

  @Test
  void elPreferidoAcepta_LaEntregaQuedaEnviadaYElOtroProveedorNiSeEntera() {
    DatosEntregaLogistica datos = pedido();
    broker.solicitarEntrega(datos);

    relay.procesarPendientes();

    verify(donatrack).enviar(any(), eq(datos), any());
    verify(externo, never()).enviar(any(), any(), any());
    assertEquals(EstadoSolicitudEntrega.ENVIADA, unicaSolicitud().getEstado());
  }

  @Test
  void elPreferidoRechaza_LaEntregaTerminaEnElSiguienteProveedor() {
    doThrow(new EnvioRechazadoException("donatrack", "sin cola"))
        .when(donatrack)
        .enviar(any(), any(), any());
    broker.solicitarEntrega(pedido());

    relay.procesarPendientes();
    relay.procesarPendientes();

    verify(externo).enviar(any(), any(), any());
    SolicitudEntrega solicitud = unicaSolicitud();
    assertEquals(EstadoSolicitudEntrega.ENVIADA, solicitud.getEstado());
    assertEquals("externo", solicitud.getProveedorActual());
  }

  @Test
  void elPreferidoNoResponde_SeReintentaConElMismoYNuncaSeLeMandaAlOtro() {
    doThrow(new EnvioInciertoException("donatrack", "timeout"))
        .doNothing()
        .when(donatrack)
        .enviar(any(), any(), any());
    broker.solicitarEntrega(pedido());

    relay.procesarPendientes();
    relay.procesarPendientes();
    verify(donatrack, times(1)).enviar(any(), any(), any());

    reloj.adelantar(Duration.ofSeconds(60));
    relay.procesarPendientes();

    verify(donatrack, times(2)).enviar(any(), any(), any());
    verify(externo, never()).enviar(any(), any(), any());
    assertEquals(EstadoSolicitudEntrega.ENVIADA, unicaSolicitud().getEstado());
  }

  @Test
  void elPreferidoNuncaResponde_LaEntregaFallaSinPasarPorElOtroProveedor() {
    doThrow(new EnvioInciertoException("donatrack", "timeout"))
        .when(donatrack)
        .enviar(any(), any(), any());
    broker.solicitarEntrega(pedido());

    for (int i = 0; i < 3; i++) {
      relay.procesarPendientes();
      reloj.adelantar(Duration.ofMinutes(10));
    }

    verify(donatrack, times(3)).enviar(any(), any(), any());
    verify(externo, never()).enviar(any(), any(), any());
    assertEquals(EstadoSolicitudEntrega.FALLIDA, unicaSolicitud().getEstado());
  }
}
