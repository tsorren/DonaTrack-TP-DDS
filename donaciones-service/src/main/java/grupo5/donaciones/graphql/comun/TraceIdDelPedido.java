package grupo5.donaciones.graphql.comun;

import graphql.GraphQLContext;
import grupo5.common.logging.TraceResponseHeaderFilter;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Deja el {@code traceId} del pedido en el contexto de GraphQL, para que los errores lo devuelvan
 * aunque el resolver corra en otro hilo. Toma el del MDC, que es el que eligió {@link
 * TraceResponseHeaderFilter}, y si no está, el encabezado {@code X-Trace-Id}.
 */
@Component
public class TraceIdDelPedido implements WebGraphQlInterceptor {

  /** La clave del {@code traceId} en el {@link GraphQLContext}. */
  public static final String CLAVE = "traceId";

  @Override
  public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
    String traceId =
        elegir(
            MDC.get(TraceResponseHeaderFilter.MDC_TRACE_KEY),
            request.getHeaders().getFirst(TraceResponseHeaderFilter.TRACE_HEADER));
    if (traceId != null) {
      request.configureExecutionInput(
          (entrada, constructor) -> constructor.graphQLContext(Map.of(CLAVE, traceId)).build());
    }
    return chain.next(request);
  }

  /** El {@code traceId} guardado en el contexto, o el del MDC; {@code null} si no hay ninguno. */
  public static String de(GraphQLContext contexto) {
    String guardado = contexto == null ? null : contexto.get(CLAVE);
    return elegir(guardado, MDC.get(TraceResponseHeaderFilter.MDC_TRACE_KEY));
  }

  private static String elegir(String primero, String segundo) {
    if (primero != null && !primero.isBlank()) {
      return primero;
    }
    return segundo == null || segundo.isBlank() ? null : segundo;
  }
}
