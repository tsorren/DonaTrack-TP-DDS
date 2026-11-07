package grupo5.common.logging;

import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;
import brave.propagation.TraceIdContext;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Decora la propagación de Brave para adoptar el {@code X-Trace-Id} entrante (ADR-E6-02, {@code
 * proposed}: lo genera quien llama, para correlacionar sus errores con los logs).
 *
 * <p>Si el pedido no trae contexto W3C ni B3, y {@code X-Trace-Id} tiene el formato de {@code
 * common-lib} (32 hex en minúscula), el span del servidor arranca con ese {@code traceId}. Así lo
 * ven el MDC, los logs y {@link TraceResponseHeaderFilter}. Con otro formato, Brave genera uno
 * nuevo, como antes.
 */
public final class XTraceIdPropagationFactory extends Propagation.Factory {

  private static final Pattern TRACE_ID_128 = Pattern.compile("[0-9a-f]{32}");

  private final Propagation.Factory delegate;

  public XTraceIdPropagationFactory(Propagation.Factory delegate) {
    this.delegate = delegate;
  }

  @Override
  public boolean supportsJoin() {
    return delegate.supportsJoin();
  }

  @Override
  public boolean requires128BitTraceId() {
    return delegate.requires128BitTraceId();
  }

  @Override
  public TraceContext decorate(TraceContext context) {
    return delegate.decorate(context);
  }

  @Override
  public Propagation<String> get() {
    Propagation<String> propagation = delegate.get();
    return new Propagation<>() {
      @Override
      public List<String> keys() {
        return propagation.keys();
      }

      @Override
      public <R> TraceContext.Injector<R> injector(Setter<R, String> setter) {
        return propagation.injector(setter);
      }

      @Override
      public <R> TraceContext.Extractor<R> extractor(Getter<R, String> getter) {
        TraceContext.Extractor<R> extractor = propagation.extractor(getter);
        return request ->
            adoptarTraceId(
                extractor.extract(request),
                getter.get(request, TraceResponseHeaderFilter.TRACE_HEADER));
      }
    };
  }

  static TraceContextOrSamplingFlags adoptarTraceId(
      TraceContextOrSamplingFlags extraido, String header) {
    if (extraido.context() != null
        || extraido.traceIdContext() != null
        || header == null
        || !TRACE_ID_128.matcher(header).matches()) {
      return extraido;
    }
    long traceIdHigh = Long.parseUnsignedLong(header.substring(0, 16), 16);
    long traceId = Long.parseUnsignedLong(header.substring(16), 16);
    if (traceIdHigh == 0L || traceId == 0L) {
      // Con la mitad alta en cero Brave lo imprimiría con 16 caracteres; con la baja, no lo acepta.
      return extraido;
    }
    // Conserva la decisión de muestreo que haya llegado (por ejemplo, b3: 0).
    TraceIdContext traceIdContext =
        TraceIdContext.newBuilder()
            .traceIdHigh(traceIdHigh)
            .traceId(traceId)
            .sampled(extraido.sampled())
            .debug(extraido.samplingFlags().debug())
            .build();
    TraceContextOrSamplingFlags.Builder builder =
        TraceContextOrSamplingFlags.newBuilder(traceIdContext);
    if (extraido.sampledLocal()) {
      builder.sampledLocal();
    }
    extraido.extra().forEach(builder::addExtra);
    return builder.build();
  }
}
