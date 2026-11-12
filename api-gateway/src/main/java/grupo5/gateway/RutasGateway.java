package grupo5.gateway;

import static org.springframework.cloud.gateway.server.mvc.filter.AfterFilterFunctions.removeResponseHeader;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;

import grupo5.common.logging.TraceResponseHeaderFilter;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RequestPredicate;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Rutas expuestas por el gateway: son la allowlist de lo que llega a los servicios. Lo que no
 * figura acá da 404 (denegar por defecto).
 *
 * <p>Sin reescritura (R1 del ADR-E6-02): la ruta llega igual al servicio. {@code /api/logistica} es
 * de dos servicios, así que se enruta por ruta completa. Quedan afuera los alias sin {@code /api}
 * de Notificaciones y {@code /donaciones-independientes}.
 */
@Configuration(proxyBeanMethods = false)
public class RutasGateway {

  private static final Logger log = LoggerFactory.getLogger(RutasGateway.class);

  @Bean
  RouterFunction<ServerResponse> rutaDonaciones(GatewayProperties props) {
    return ruta(
        "donaciones",
        props.servicios().donaciones(),
        "/api/actividad",
        "/api/categorias/**",
        "/api/donaciones/**",
        "/api/donantes/**",
        "/api/entidades/**",
        "/api/items-normalizados/**",
        "/api/necesidades/**",
        "/api/personas/**",
        "/api/asignaciones/**",
        "/api/subcategorias/**",
        "/api/logistica/proveedores",
        "/api/logistica/proveedor-preferido",
        // Merge local (demo): el cliente liviano llama a esta ruta sin /api.
        "/donaciones-independientes/**");
  }

  @Bean
  RouterFunction<ServerResponse> rutaNotificaciones(GatewayProperties props) {
    return ruta(
        "notificaciones",
        props.servicios().notificaciones(),
        "/api/notificaciones/**",
        "/api/contacto",
        "/api/contacto/**");
  }

  @Bean
  RouterFunction<ServerResponse> rutaIncentivos(GatewayProperties props) {
    return ruta("incentivos", props.servicios().incentivos(), "/api/incentivos/**");
  }

  @Bean
  RouterFunction<ServerResponse> rutaLogistica(GatewayProperties props) {
    return ruta(
        "logistica",
        props.servicios().logistica(),
        "/api/camiones/**",
        "/api/choferes/**",
        "/api/entregas/**",
        "/api/rutas/**",
        "/api/logistica/resultados",
        "/api/logistica/callback/**",
        "/api/logistica/planificaciones/**");
  }

  private static RouterFunction<ServerResponse> ruta(
      String servicio, String url, String... patrones) {
    RequestPredicate predicado =
        Arrays.stream(patrones)
            .map(RequestPredicates::path)
            .reduce(RequestPredicate::or)
            .orElseThrow();
    return route(servicio)
        .route(predicado, http())
        .before(uri(url))
        .before(RutasGateway::propagarTraceId)
        // El X-Trace-Id de la respuesta lo pone el filtro de common-lib; el del servicio lo
        // duplicaría.
        .after(removeResponseHeader(TraceResponseHeaderFilter.TRACE_HEADER))
        .after(
            (request, response) -> {
              log.info(
                  "[GATEWAY] [VERB: {}] [PATH: {}] [SERVICE: {}] [STATUS: {}]",
                  request.method(),
                  request.path(),
                  servicio,
                  response.statusCode().value());
              return response;
            })
        .build();
  }

  /**
   * Manda al servicio el traceId del pedido: el del cliente, o el que generó el gateway si no vino
   * (lo deja en el MDC el {@link TraceResponseHeaderFilter} de common-lib).
   */
  static ServerRequest propagarTraceId(ServerRequest request) {
    // El filtro corre antes que el gateway y siempre deja el traceId en el MDC.
    String traceId = MDC.get(TraceResponseHeaderFilter.MDC_TRACE_KEY);
    return ServerRequest.from(request)
        .headers(h -> h.set(TraceResponseHeaderFilter.TRACE_HEADER, traceId))
        .build();
  }
}
