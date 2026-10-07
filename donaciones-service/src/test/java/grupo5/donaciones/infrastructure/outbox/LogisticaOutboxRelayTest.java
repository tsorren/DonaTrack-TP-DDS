package grupo5.donaciones.infrastructure.outbox;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import grupo5.common.logging.FeignTraceRequestInterceptor;
import grupo5.donaciones.dto.logistica.DatosEntregaLogistica;
import grupo5.donaciones.services.logistica.EntradaOutboxLogistica;
import grupo5.donaciones.services.logistica.EnvioInciertoException;
import grupo5.donaciones.services.logistica.EnvioRechazadoException;
import grupo5.donaciones.services.logistica.ErrorContratoProveedorException;
import grupo5.donaciones.services.logistica.ILogisticaBroker;
import grupo5.donaciones.services.logistica.IProveedorLogistica;
import grupo5.donaciones.services.logistica.ResultadoEnvio;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;

class LogisticaOutboxRelayTest {

  private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 7, 12, 0);
  private static final Clock CLOCK = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));

  private LogisticaOutboxEnMemoria outbox;
  private ILogisticaBroker broker;

  /** Proveedor de prueba: ejecuta la acción configurada y recuerda el traceId que vio en el MDC. */
  private static final class ProveedorFake implements IProveedorLogistica {
    private final String id;
    private final Runnable accion;
    private final List<String> traceIdsVistos = new ArrayList<>();

    ProveedorFake(String id, Runnable accion) {
      this.id = id;
      this.accion = accion;
    }

    @Override
    public String id() {
      return id;
    }

    @Override
    public void enviar(UUID envioId, DatosEntregaLogistica datos, String traceId) {
      traceIdsVistos.add(MDC.get(FeignTraceRequestInterceptor.MDC_TRACE_KEY));
      accion.run();
    }
  }

  @BeforeEach
  void setUp() {
    outbox = new LogisticaOutboxEnMemoria();
    broker = mock(ILogisticaBroker.class);
  }

  @AfterEach
  void limpiarMdc() {
    MDC.clear();
  }

  @SuppressWarnings("unchecked")
  private LogisticaOutboxRelay relayCon(IProveedorLogistica... proveedores) {
    ObjectProvider<IProveedorLogistica> provider = mock(ObjectProvider.class);
    when(provider.orderedStream()).thenReturn(Stream.of(proveedores));
    return new LogisticaOutboxRelay(outbox, broker, provider, CLOCK);
  }

  private EntradaOutboxLogistica encolar(String proveedorId, LocalDateTime ahora) {
    EntradaOutboxLogistica entrada =
        EntradaOutboxLogistica.nueva(UUID.randomUUID(), proveedorId, null, "trace-x", 5, ahora);
    outbox.guardar(entrada);
    return entrada;
  }

  private ResultadoEnvio resultadoCuandoElProveedor(Supplier<RuntimeException> falla) {
    Runnable accion =
        () -> {
          if (falla != null) {
            throw falla.get();
          }
        };
    var relay = relayCon(new ProveedorFake("donatrack", accion));
    EntradaOutboxLogistica entrada = encolar("donatrack", AHORA);

    relay.procesarPendientes();

    ArgumentCaptor<ResultadoEnvio> captor = ArgumentCaptor.forClass(ResultadoEnvio.class);
    verify(broker).registrarResultado(eq(entrada.getId()), captor.capture());
    return captor.getValue();
  }

  @Test
  void deberiaInformarPublicado_CuandoElProveedorAceptaElPedido() {
    assertEquals(ResultadoEnvio.PUBLICADO, resultadoCuandoElProveedor(null));
  }

  @Test
  void deberiaInformarRechazado_CuandoElProveedorRechaza() {
    assertEquals(
        ResultadoEnvio.RECHAZADO,
        resultadoCuandoElProveedor(() -> new EnvioRechazadoException("donatrack", "sin cola")));
  }

  @Test
  void deberiaInformarIncierto_CuandoNoSeSabeSiLlego() {
    assertEquals(
        ResultadoEnvio.INCIERTO,
        resultadoCuandoElProveedor(() -> new EnvioInciertoException("donatrack", "timeout")));
  }

  @Test
  void deberiaInformarErrorDeContrato_CuandoElProveedorRechazaElContenido() {
    assertEquals(
        ResultadoEnvio.ERROR_CONTRATO,
        resultadoCuandoElProveedor(
            () -> new ErrorContratoProveedorException("donatrack", "400 Bad Request")));
  }

  @Test
  void deberiaInformarIncierto_CuandoElProveedorLanzaUnaExcepcionNoClasificada() {
    assertEquals(
        ResultadoEnvio.INCIERTO,
        resultadoCuandoElProveedor(() -> new IllegalStateException("bug")));
  }

  @Test
  void deberiaInformarRechazado_CuandoElProveedorNoTieneAdapter() {
    var relay = relayCon();
    EntradaOutboxLogistica entrada = encolar("externo", AHORA);

    relay.procesarPendientes();

    verify(broker).registrarResultado(entrada.getId(), ResultadoEnvio.RECHAZADO);
  }

  @Test
  void deberiaProcesarSoloLasEntradasListas() {
    var relay = relayCon(new ProveedorFake("donatrack", () -> {}));
    EntradaOutboxLogistica lista = encolar("donatrack", AHORA);
    encolar("donatrack", AHORA.plusMinutes(5));

    relay.procesarPendientes();

    verify(broker).registrarResultado(lista.getId(), ResultadoEnvio.PUBLICADO);
    verifyNoMoreInteractions(broker);
  }

  @Test
  void unErrorDelBrokerNoDeberiaCortarElProcesamientoDelResto() {
    var relay = relayCon(new ProveedorFake("donatrack", () -> {}));
    EntradaOutboxLogistica primera = encolar("donatrack", AHORA.minusSeconds(2));
    EntradaOutboxLogistica segunda = encolar("donatrack", AHORA.minusSeconds(1));
    doThrow(new IllegalStateException("falla del broker"))
        .when(broker)
        .registrarResultado(eq(primera.getId()), any());

    relay.procesarPendientes();

    verify(broker).registrarResultado(segunda.getId(), ResultadoEnvio.PUBLICADO);
  }

  @Test
  void deberiaPropagarElTraceIdDeLaEntradaYRestaurarElAnterior() {
    ProveedorFake proveedor = new ProveedorFake("donatrack", () -> {});
    var relay = relayCon(proveedor);
    encolar("donatrack", AHORA);
    MDC.put(FeignTraceRequestInterceptor.MDC_TRACE_KEY, "trace-del-job");

    relay.procesarPendientes();

    assertEquals(List.of("trace-x"), proveedor.traceIdsVistos);
    assertEquals("trace-del-job", MDC.get(FeignTraceRequestInterceptor.MDC_TRACE_KEY));
  }
}
