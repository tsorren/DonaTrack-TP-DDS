package grupo5.cliente.rest;

import grupo5.cliente.web.TraceIdFiltro;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.server.PathContainer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Un {@link RestClient} por API, con su URL base ({@code cliente.api.<api>.url}). Cada llamada
 * lleva el {@code X-Trace-Id} del pedido del navegador (ADR-E6-02: lo genera el cliente) y la IP
 * del navegador en {@code X-Forwarded-For}, para que el rate limiting del gateway (E7, ADR
 * 20261115) cuente a cada visitante por separado y no a todos como la IP del cliente liviano. Las
 * de las cuatro APIs llevan además el token de quien tiene la sesión (fuera de la maqueta); sin
 * ingreso, como en la portada, el mapa público o el contacto, el interceptor no pone cabecera y la
 * llamada va sin token, y la API decide si ese endpoint es público.
 *
 * <p>Ninguna llamada espera sin límite (R11-04): si la API no conecta o no responde a tiempo, la
 * llamada falla con una {@code ResourceAccessException} y {@code ErroresHttp} la lleva a la página
 * de falla con su código de soporte. Los límites son {@code cliente.http.*}; la subida de un
 * archivo tiene el suyo, más largo (ver {@link Esperas}).
 */
@Configuration
class ClientesHttp {

  /** Nombre del bean con el interceptor del token del usuario (ver {@code IntegracionConfig}). */
  private static final String TOKEN_DEL_USUARIO = "tokenDelUsuario";

  static final String X_FORWARDED_FOR = "X-Forwarded-For";

  private final Supplier<RestClient.Builder> constructores;

  @Autowired
  ClientesHttp(
      @Value("${cliente.http.timeout-conexion}") Duration conexion,
      @Value("${cliente.http.timeout-lectura}") Duration lectura,
      @Value("${cliente.http.timeout-subida}") Duration subida) {
    ClientHttpRequestFactory fabrica = new Esperas(conexion, lectura, subida).fabrica();
    this.constructores = () -> RestClient.builder().requestFactory(fabrica);
  }

  /** Los tests pasan un constructor atado a {@code MockRestServiceServer}. */
  ClientesHttp(Supplier<RestClient.Builder> constructores) {
    this.constructores = constructores;
  }

  @Bean
  Clock reloj() {
    return Clock.systemDefaultZone();
  }

  @Bean
  IncentivosApi incentivosApi(
      @Value("${cliente.api.incentivos.url}") String url,
      @Qualifier(TOKEN_DEL_USUARIO) ObjectProvider<ClientHttpRequestInterceptor> tokenDelUsuario) {
    return cliente(constructores.get(), url, IncentivosApi.class, interceptores(tokenDelUsuario));
  }

  @Bean
  DonacionesApi donacionesApi(
      @Value("${cliente.api.donaciones.url}") String url,
      @Qualifier(TOKEN_DEL_USUARIO) ObjectProvider<ClientHttpRequestInterceptor> tokenDelUsuario) {
    return cliente(constructores.get(), url, DonacionesApi.class, interceptores(tokenDelUsuario));
  }

  @Bean
  LogisticaApi logisticaApi(
      @Value("${cliente.api.logistica.url}") String url,
      @Qualifier(TOKEN_DEL_USUARIO) ObjectProvider<ClientHttpRequestInterceptor> tokenDelUsuario) {
    return cliente(constructores.get(), url, LogisticaApi.class, interceptores(tokenDelUsuario));
  }

  @Bean
  NotificacionesApi notificacionesApi(
      @Value("${cliente.api.notificaciones.url}") String url,
      @Qualifier(TOKEN_DEL_USUARIO) ObjectProvider<ClientHttpRequestInterceptor> tokenDelUsuario) {
    return cliente(
        constructores.get(), url, NotificacionesApi.class, interceptores(tokenDelUsuario));
  }

  /** Vacío en la maqueta, que no tiene ingreso: ahí ninguna llamada lleva token. */
  private static ClientHttpRequestInterceptor[] interceptores(
      ObjectProvider<ClientHttpRequestInterceptor> tokenDelUsuario) {
    return tokenDelUsuario.stream().toArray(ClientHttpRequestInterceptor[]::new);
  }

  /** Arma el proxy de una API; los tests le pasan el builder atado a MockRestServiceServer. */
  static <T> T cliente(
      RestClient.Builder builder,
      String url,
      Class<T> api,
      ClientHttpRequestInterceptor... interceptores) {
    RestClient restClient =
        builder
            .baseUrl(url)
            .requestInterceptor(
                (pedido, cuerpo, ejecucion) -> {
                  String traceId = MDC.get(TraceIdFiltro.CLAVE);
                  if (traceId != null) {
                    pedido.getHeaders().set(TraceIdFiltro.CABECERA, traceId);
                  }
                  // El contacto ya la pone por su cuenta (HB-21): no se pisa.
                  String ip = ipDelNavegador();
                  if (ip != null && pedido.getHeaders().getFirst(X_FORWARDED_FOR) == null) {
                    pedido.getHeaders().set(X_FORWARDED_FOR, ip);
                  }
                  return ejecucion.execute(pedido, cuerpo);
                })
            .requestInterceptors(lista -> lista.addAll(List.of(interceptores)))
            .build();
    return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
        .build()
        .createClient(api);
  }

  /**
   * La IP de quien le habla al cliente liviano, o null si la llamada no sale de un pedido del
   * navegador. Es {@code getRemoteAddr()}: ningún despliegue pone un proxy delante del cliente.
   */
  static String ipDelNavegador() {
    return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes pedido
        ? pedido.getRequest().getRemoteAddr()
        : null;
  }

  /**
   * Los límites de espera. {@code conexion}: lo que tarda en abrirse la conexión. {@code lectura}:
   * lo que tarda la API en empezar a responder. {@code subida}: lo mismo, pero para las llamadas
   * que suben un archivo (el CSV de donantes y las fotos del acta), que mueven hasta 50 MB y
   * necesitan más.
   */
  record Esperas(Duration conexion, Duration lectura, Duration subida) {

    /** Las subidas de archivos: se reconocen por método y ruta (la API es una por servicio). */
    private static final List<PathPattern> RUTAS_DE_SUBIDA =
        List.of("/api/donantes/archivos", "/api/entregas/{id}/fotos").stream()
            .map(PathPatternParser.defaultInstance::parse)
            .toList();

    static boolean esSubida(URI uri, HttpMethod metodo) {
      PathContainer ruta = PathContainer.parsePath(uri.getRawPath());
      return HttpMethod.POST.equals(metodo)
          && RUTAS_DE_SUBIDA.stream().anyMatch(patron -> patron.matches(ruta));
    }

    /** Una fábrica de pedidos que aplica el límite que corresponde a cada llamada. */
    ClientHttpRequestFactory fabrica() {
      HttpClient http = HttpClient.newBuilder().connectTimeout(conexion).build();
      JdkClientHttpRequestFactory comun = new JdkClientHttpRequestFactory(http);
      comun.setReadTimeout(lectura);
      JdkClientHttpRequestFactory deSubida = new JdkClientHttpRequestFactory(http);
      deSubida.setReadTimeout(subida);
      return new ClientHttpRequestFactory() {
        @Override
        public ClientHttpRequest createRequest(URI uri, HttpMethod metodo) throws IOException {
          return (esSubida(uri, metodo) ? deSubida : comun).createRequest(uri, metodo);
        }
      };
    }
  }
}
