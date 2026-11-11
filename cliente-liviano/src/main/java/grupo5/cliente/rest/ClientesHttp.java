package grupo5.cliente.rest;

import grupo5.cliente.web.TraceIdFiltro;
import java.time.Clock;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Un {@link RestClient} por API, con su URL base ({@code cliente.api.<api>.url}). Cada llamada
 * lleva el {@code X-Trace-Id} del pedido del navegador (ADR-E6-02: lo genera el cliente). Las de
 * las cuatro APIs llevan además el token de quien tiene la sesión (fuera de la maqueta); sin
 * ingreso, como en la portada, el mapa público o el contacto, el interceptor no pone cabecera y la
 * llamada va sin token, y la API decide si ese endpoint es público.
 */
@Configuration
class ClientesHttp {

  private final Supplier<RestClient.Builder> constructores;

  ClientesHttp() {
    this(RestClient::builder);
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
      @Qualifier("tokenDelUsuario") ObjectProvider<ClientHttpRequestInterceptor> tokenDelUsuario) {
    return cliente(constructores.get(), url, IncentivosApi.class, interceptores(tokenDelUsuario));
  }

  @Bean
  DonacionesApi donacionesApi(
      @Value("${cliente.api.donaciones.url}") String url,
      @Qualifier("tokenDelUsuario") ObjectProvider<ClientHttpRequestInterceptor> tokenDelUsuario) {
    return cliente(constructores.get(), url, DonacionesApi.class, interceptores(tokenDelUsuario));
  }

  @Bean
  LogisticaApi logisticaApi(
      @Value("${cliente.api.logistica.url}") String url,
      @Qualifier("tokenDelUsuario") ObjectProvider<ClientHttpRequestInterceptor> tokenDelUsuario) {
    return cliente(constructores.get(), url, LogisticaApi.class, interceptores(tokenDelUsuario));
  }

  @Bean
  NotificacionesApi notificacionesApi(
      @Value("${cliente.api.notificaciones.url}") String url,
      @Qualifier("tokenDelUsuario") ObjectProvider<ClientHttpRequestInterceptor> tokenDelUsuario) {
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
                  return ejecucion.execute(pedido, cuerpo);
                })
            .requestInterceptors(lista -> lista.addAll(List.of(interceptores)))
            .build();
    return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
        .build()
        .createClient(api);
  }
}
