package grupo5.common.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.tracing.autoconfigure.MicrometerTracingAutoConfiguration;
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.OpenTelemetryEventPublisherBeansApplicationListener;
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.OpenTelemetryTracingAutoConfiguration;
import org.springframework.boot.opentelemetry.autoconfigure.OpenTelemetrySdkAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class XTraceIdPropagatorTest {

  private static final String HEADER = TraceResponseHeaderFilter.TRACE_HEADER;
  private static final String TRACE_ID_CLIENTE = "0123456789abcdef0123456789abcdef";
  private static final TextMapGetter<Map<String, String>> GETTER =
      new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
          return carrier.keySet();
        }

        @Override
        public String get(Map<String, String> carrier, String key) {
          return carrier == null ? null : carrier.get(key);
        }
      };

  private final TextMapPropagator propagador =
      new XTraceIdPropagator(W3CTraceContextPropagator.getInstance());
  private SdkTracerProvider proveedor;
  private io.opentelemetry.api.trace.Tracer tracer;

  @BeforeEach
  void setUp() {
    MDC.clear();
    proveedor = SdkTracerProvider.builder().build();
    tracer = proveedor.get("test");
  }

  @AfterEach
  void tearDown() {
    proveedor.close();
    MDC.clear();
  }

  /** El span del servidor que arranca con lo extraído de los headers. */
  private SpanContext spanDelServidor(Map<String, String> headers) {
    Context padre = propagador.extract(Context.root(), headers, GETTER);
    Span span = tracer.spanBuilder("servidor").setParent(padre).startSpan();
    span.end();
    return span.getSpanContext();
  }

  @Test
  void extract_conXTraceIdValido_deberiaArrancarElSpanConEseTraceIdYMuestreado() {
    SpanContext span = spanDelServidor(Map.of(HEADER, TRACE_ID_CLIENTE));

    assertEquals(TRACE_ID_CLIENTE, span.getTraceId());
    assertTrue(span.isSampled());
  }

  @Test
  void extract_conTraceparentYXTraceId_deberiaRespetarElContextoW3C() {
    Map<String, String> headers = new HashMap<>();
    headers.put(HEADER, TRACE_ID_CLIENTE);
    headers.put("traceparent", "00-fedcba9876543210fedcba9876543210-0123456789abcdef-01");

    assertEquals("fedcba9876543210fedcba9876543210", spanDelServidor(headers).getTraceId());
  }

  @Test
  void extract_conXTraceIdSinFormatoHex_deberiaGenerarOtroTraceId() {
    SpanContext span = spanDelServidor(Map.of(HEADER, "incoming-trace-abc"));

    assertNotEquals("incoming-trace-abc", span.getTraceId());
    assertEquals(32, span.getTraceId().length());
  }

  @Test
  void extract_conXTraceIdEnMayusculas_deberiaGenerarOtroTraceId() {
    String mayusculas = TRACE_ID_CLIENTE.toUpperCase();

    SpanContext span = spanDelServidor(Map.of(HEADER, mayusculas));

    assertNotEquals(mayusculas, span.getTraceId());
    assertNotEquals(TRACE_ID_CLIENTE, span.getTraceId());
  }

  @Test
  void adoptarTraceId_conContextoYaExtraido_deberiaRespetarlo() {
    SpanContext remoto =
        SpanContext.createFromRemoteParent(
            "fedcba9876543210fedcba9876543210",
            "0123456789abcdef",
            TraceFlags.getSampled(),
            TraceState.getDefault());
    Context extraido = Context.root().with(Span.wrap(remoto));

    assertSame(extraido, XTraceIdPropagator.adoptarTraceId(extraido, TRACE_ID_CLIENTE));
  }

  @Test
  void extract_conMitadAltaEnCero_deberiaGenerarOtroTraceId() {
    String mitadAltaEnCero = "00000000000000000123456789abcdef";

    SpanContext span = spanDelServidor(Map.of(HEADER, mitadAltaEnCero));

    assertNotEquals(mitadAltaEnCero, span.getTraceId());
    assertEquals(32, span.getTraceId().length());
  }

  @Test
  void extract_conMitadBajaEnCero_deberiaGenerarOtroTraceIdSinFallar() {
    String mitadBajaEnCero = "0123456789abcdef0000000000000000";

    SpanContext span = spanDelServidor(Map.of(HEADER, mitadBajaEnCero));

    assertNotEquals(mitadBajaEnCero, span.getTraceId());
    assertEquals(32, span.getTraceId().length());
  }

  @Test
  void contextoDeBoot_deberiaEnvolverLaPropagacionYCargarElTraceIdEnElMdc() {
    // En una aplicación lo registra spring.factories al arrancar; ApplicationContextRunner no lo
    // carga. Es el que lleva los eventos del contexto de OpenTelemetry al Slf4JEventListener (MDC).
    OpenTelemetryEventPublisherBeansApplicationListener.addWrapper();
    new ApplicationContextRunner()
        .withInitializer(
            contexto ->
                contexto.addApplicationListener(
                    new OpenTelemetryEventPublisherBeansApplicationListener()))
        .withConfiguration(
            AutoConfigurations.of(
                OpenTelemetrySdkAutoConfiguration.class,
                OpenTelemetryTracingAutoConfiguration.class,
                MicrometerTracingAutoConfiguration.class,
                LoggingAutoConfiguration.XTraceIdPropagationConfiguration.class))
        .run(
            context -> {
              assertInstanceOf(XTraceIdPropagator.class, context.getBean(TextMapPropagator.class));
              Tracer tracerDeBoot = context.getBean(Tracer.class);
              io.micrometer.tracing.Span span =
                  context
                      .getBean(Propagator.class)
                      .extract(Map.of(HEADER, TRACE_ID_CLIENTE), Map::get)
                      .start();
              try (Tracer.SpanInScope scope = tracerDeBoot.withSpan(span)) {
                assertEquals(TRACE_ID_CLIENTE, span.context().traceId());
                assertEquals(TRACE_ID_CLIENTE, MDC.get(TraceResponseHeaderFilter.MDC_TRACE_KEY));
              } finally {
                span.end();
              }
            });
  }

  @Test
  void postProcessor_deberiaEnvolverSoloElPropagador() {
    BeanPostProcessor postProcessor =
        LoggingAutoConfiguration.XTraceIdPropagationConfiguration
            .xTraceIdPropagationPostProcessor();
    Object envuelto =
        postProcessor.postProcessAfterInitialization(
            W3CTraceContextPropagator.getInstance(), "textMapPropagator");
    Object otro = new Object();

    assertInstanceOf(XTraceIdPropagator.class, envuelto);
    assertSame(envuelto, postProcessor.postProcessAfterInitialization(envuelto, "otraVez"));
    assertSame(otro, postProcessor.postProcessAfterInitialization(otro, "otroBean"));
  }

  @Test
  void extract_sinHeaders_deberiaGenerarUnTraceIdNuevoSinPadre() {
    Context padre = propagador.extract(Context.root(), Map.of(), GETTER);
    SpanContext span = spanDelServidor(Map.of());

    assertFalse(Span.fromContext(padre).getSpanContext().isValid());
    assertEquals(32, span.getTraceId().length());
  }

  @Test
  void extract_conCarrierNulo_deberiaDevolverLoQueExtraeElDelegado() {
    assertSame(Context.root(), propagador.extract(Context.root(), null, GETTER));
  }
}
