package grupo5.common.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.context.slf4j.MDCScopeDecorator;
import brave.propagation.B3Propagation;
import brave.propagation.CurrentTraceContext;
import brave.propagation.Propagation;
import brave.propagation.ThreadLocalCurrentTraceContext;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;
import brave.propagation.TraceIdContext;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.tracing.brave.autoconfigure.BraveAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class XTraceIdPropagationFactoryTest {

  private static final String HEADER = TraceResponseHeaderFilter.TRACE_HEADER;
  private static final String TRACE_ID_CLIENTE = "0123456789abcdef0123456789abcdef";

  private Tracing tracing;
  private Tracer tracer;
  private TraceContext.Extractor<Map<String, String>> extractor;

  @BeforeEach
  void setUp() {
    MDC.clear();
    tracing =
        Tracing.newBuilder()
            .traceId128Bit(true)
            .propagationFactory(new XTraceIdPropagationFactory(B3Propagation.FACTORY))
            .currentTraceContext(
                ThreadLocalCurrentTraceContext.newBuilder()
                    .addScopeDecorator(MDCScopeDecorator.get())
                    .build())
            .build();
    tracer = tracing.tracer();
    extractor = tracing.propagation().extractor(Map::get);
  }

  @AfterEach
  void tearDown() {
    tracing.close();
    MDC.clear();
  }

  @Test
  void extract_conXTraceIdValido_deberiaArrancarElSpanConEseTraceIdYCargarloEnElMdc() {
    Span span = tracer.nextSpan(extractor.extract(Map.of(HEADER, TRACE_ID_CLIENTE)));

    assertEquals(TRACE_ID_CLIENTE, span.context().traceIdString());
    try (CurrentTraceContext.Scope scope = tracing.currentTraceContext().newScope(span.context())) {
      assertEquals(TRACE_ID_CLIENTE, MDC.get(TraceResponseHeaderFilter.MDC_TRACE_KEY));
    }
  }

  @Test
  void extract_conB3YXTraceId_deberiaRespetarElContextoB3() {
    Map<String, String> headers = new HashMap<>();
    headers.put(HEADER, TRACE_ID_CLIENTE);
    headers.put("b3", "fedcba9876543210fedcba9876543210-0123456789abcdef-1");

    Span span = tracer.nextSpan(extractor.extract(headers));

    assertEquals("fedcba9876543210fedcba9876543210", span.context().traceIdString());
  }

  @Test
  void extract_conXTraceIdSinFormatoHex_deberiaGenerarOtroTraceId() {
    Span span = tracer.nextSpan(extractor.extract(Map.of(HEADER, "incoming-trace-abc")));

    assertNotEquals("incoming-trace-abc", span.context().traceIdString());
    assertEquals(32, span.context().traceIdString().length());
  }

  @Test
  void extract_conXTraceIdEnMayusculas_deberiaGenerarOtroTraceId() {
    String mayusculas = TRACE_ID_CLIENTE.toUpperCase();

    Span span = tracer.nextSpan(extractor.extract(Map.of(HEADER, mayusculas)));

    assertNotEquals(mayusculas, span.context().traceIdString());
    assertNotEquals(TRACE_ID_CLIENTE, span.context().traceIdString());
  }

  @Test
  void adoptarTraceId_conTraceIdContextYaExtraido_deberiaRespetarlo() {
    TraceContextOrSamplingFlags extraido =
        TraceContextOrSamplingFlags.create(
            TraceIdContext.newBuilder().traceIdHigh(1L).traceId(2L).build());

    assertSame(extraido, XTraceIdPropagationFactory.adoptarTraceId(extraido, TRACE_ID_CLIENTE));
  }

  @Test
  void extract_conMitadAltaEnCero_deberiaGenerarOtroTraceId() {
    String mitadAltaEnCero = "00000000000000000123456789abcdef";

    Span span = tracer.nextSpan(extractor.extract(Map.of(HEADER, mitadAltaEnCero)));

    assertNotEquals("0123456789abcdef", span.context().traceIdString());
    assertNotEquals(mitadAltaEnCero, span.context().traceIdString());
  }

  @Test
  void extract_conMitadBajaEnCero_deberiaGenerarOtroTraceIdSinFallar() {
    String mitadBajaEnCero = "0123456789abcdef0000000000000000";

    Span span = tracer.nextSpan(extractor.extract(Map.of(HEADER, mitadBajaEnCero)));

    assertNotEquals(mitadBajaEnCero, span.context().traceIdString());
    assertEquals(32, span.context().traceIdString().length());
  }

  @Test
  void extract_conB3NoMuestreadoYXTraceId_deberiaAdoptarloSinMuestrear() {
    Span span = tracer.nextSpan(extractor.extract(Map.of(HEADER, TRACE_ID_CLIENTE, "b3", "0")));

    assertEquals(TRACE_ID_CLIENTE, span.context().traceIdString());
    assertEquals(Boolean.FALSE, span.context().sampled());
  }

  @Test
  void contextoDeBoot_deberiaEnvolverLaPropagacionDeBrave() {
    new ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                BraveAutoConfiguration.class,
                LoggingAutoConfiguration.XTraceIdPropagationConfiguration.class))
        .run(
            context -> {
              Tracing tracingDeBoot = context.getBean(Tracing.class);
              Span span =
                  tracingDeBoot
                      .tracer()
                      .nextSpan(
                          tracingDeBoot
                              .propagation()
                              .<Map<String, String>>extractor(Map::get)
                              .extract(Map.of(HEADER, TRACE_ID_CLIENTE)));

              assertInstanceOf(
                  XTraceIdPropagationFactory.class, context.getBean(Propagation.Factory.class));
              assertEquals(TRACE_ID_CLIENTE, span.context().traceIdString());
            });
  }

  @Test
  void postProcessor_deberiaEnvolverSoloLaFabricaDePropagacion() {
    BeanPostProcessor postProcessor =
        LoggingAutoConfiguration.XTraceIdPropagationConfiguration
            .xTraceIdPropagationPostProcessor();
    Object envuelta =
        postProcessor.postProcessAfterInitialization(B3Propagation.FACTORY, "propagationFactory");
    Object otro = new Object();

    assertInstanceOf(XTraceIdPropagationFactory.class, envuelta);
    assertSame(envuelta, postProcessor.postProcessAfterInitialization(envuelta, "otraVez"));
    assertSame(otro, postProcessor.postProcessAfterInitialization(otro, "otroBean"));
  }

  @Test
  void extract_sinHeaders_deberiaGenerarUnTraceIdNuevo() {
    Span span = tracer.nextSpan(extractor.extract(Map.of()));

    assertEquals(32, span.context().traceIdString().length());
    assertNull(span.context().parentIdString());
  }
}
