package grupo5.common.logging;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.sdk.trace.IdGenerator;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Decora la propagación de OpenTelemetry para adoptar el {@code X-Trace-Id} entrante (ADR-E6-02,
 * {@code proposed}: lo genera quien llama, para correlacionar sus errores con los logs).
 *
 * <p>Si el pedido no trae contexto W3C ({@code traceparent}) y {@code X-Trace-Id} tiene el formato
 * de {@code common-lib} (32 hex en minúscula, sin mitades en cero), el span del servidor arranca
 * con ese {@code traceId}: así lo ven el MDC, los logs y {@link TraceResponseHeaderFilter}. Con
 * otro formato, OpenTelemetry genera uno nuevo, como antes.
 *
 * <p>OpenTelemetry no deja fijar un {@code traceId} sin un padre: se arma un padre remoto con un
 * {@code spanId} nuevo y muestreado, así que Tempo muestra la raíz con un padre que no conoce.
 */
public final class XTraceIdPropagator implements TextMapPropagator {

  private static final Pattern TRACE_ID_128 = Pattern.compile("[0-9a-f]{32}");
  private static final String MITAD_EN_CERO = "0000000000000000";

  private final TextMapPropagator delegate;

  public XTraceIdPropagator(TextMapPropagator delegate) {
    this.delegate = delegate;
  }

  @Override
  public Collection<String> fields() {
    return delegate.fields();
  }

  @Override
  public <C> void inject(Context context, C carrier, TextMapSetter<C> setter) {
    delegate.inject(context, carrier, setter);
  }

  @Override
  public <C> Context extract(Context context, C carrier, TextMapGetter<C> getter) {
    Context extraido = delegate.extract(context, carrier, getter);
    if (carrier == null) {
      return extraido;
    }
    return adoptarTraceId(extraido, getter.get(carrier, TraceResponseHeaderFilter.TRACE_HEADER));
  }

  static Context adoptarTraceId(Context extraido, String header) {
    if (Span.fromContext(extraido).getSpanContext().isValid()
        || header == null
        || !TRACE_ID_128.matcher(header).matches()
        || header.startsWith(MITAD_EN_CERO)
        || header.endsWith(MITAD_EN_CERO)) {
      return extraido;
    }
    SpanContext padre =
        SpanContext.createFromRemoteParent(
            header,
            IdGenerator.random().generateSpanId(),
            TraceFlags.getSampled(),
            TraceState.getDefault());
    return extraido.with(Span.wrap(padre));
  }
}
