package grupo5.donaciones.graphql.limites;

import graphql.ExecutionResult;
import grupo5.common.logging.TraceResponseHeaderFilter;
import grupo5.donaciones.graphql.comun.TraceIdDelPedido;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.graphql.support.DefaultExecutionGraphQlResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * El tiempo máximo de una consulta (SPEC-11, G3), leído de {@code
 * donatrack.graphql.limites.tiempo-maximo-ms}. Queda por debajo de la espera de lectura del gateway
 * (10 s), así el que corta es Donaciones y el cliente recibe un error de GraphQL con {@code
 * ERR-CSR-500}, el motivo {@code tiempo} y el {@code traceId}, en vez de un 504 del gateway.
 *
 * <p>Es el {@code TimeoutWebGraphQlInterceptor} de Spring for GraphQL, salvo la respuesta: aquel
 * contesta un 408 sin cuerpo.
 */
@Component
public class LimiteDeTiempo implements WebGraphQlInterceptor {

  private final Duration tiempoMaximo;
  private final RechazosDeConsulta rechazos;

  public LimiteDeTiempo(
      @Value("${donatrack.graphql.limites.tiempo-maximo-ms}") long tiempoMaximoMs,
      RechazosDeConsulta rechazos) {
    if (tiempoMaximoMs < 1) {
      throw new IllegalArgumentException("El tiempo máximo de GraphQL tiene que ser positivo");
    }
    this.tiempoMaximo = Duration.ofMillis(tiempoMaximoMs);
    this.rechazos = rechazos;
  }

  @Override
  public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest pedido, Chain cadena) {
    String traceId = traceIdDe(pedido);
    return cadena
        .next(pedido)
        .timeout(tiempoMaximo, Mono.fromSupplier(() -> agotado(pedido, traceId)));
  }

  private WebGraphQlResponse agotado(WebGraphQlRequest pedido, String traceId) {
    rechazos.contar(MotivoDeRechazo.TIEMPO);
    ExecutionResult resultado =
        ExecutionResult.newExecutionResult()
            .addError(
                MotivoDeRechazo.TIEMPO.error(
                    String.format(
                        "La consulta superó el tiempo máximo de %d ms", tiempoMaximo.toMillis()),
                    traceId))
            .build();
    return new WebGraphQlResponse(
        new DefaultExecutionGraphQlResponse(pedido.toExecutionInput(), resultado));
  }

  /** El del MDC, que eligió el filtro de trazas, o el encabezado {@code X-Trace-Id}. */
  private static String traceIdDe(WebGraphQlRequest pedido) {
    String delMdc = TraceIdDelPedido.de(null);
    return delMdc != null
        ? delMdc
        : pedido.getHeaders().getFirst(TraceResponseHeaderFilter.TRACE_HEADER);
  }
}
