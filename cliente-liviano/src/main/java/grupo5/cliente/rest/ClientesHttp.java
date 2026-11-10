package grupo5.cliente.rest;

import grupo5.cliente.web.TraceIdFiltro;
import java.time.Clock;
import java.util.List;
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
 * Donaciones llevan además el token de quien tiene la sesión, si hay ingreso (fuera de la maqueta).
 */
@Configuration
class ClientesHttp {

  @Bean
  Clock reloj() {
    return Clock.systemDefaultZone();
  }

  @Bean
  IncentivosApi incentivosApi(@Value("${cliente.api.incentivos.url}") String url) {
    return cliente(RestClient.builder(), url, IncentivosApi.class);
  }

  @Bean
  DonacionesApi donacionesApi(
      @Value("${cliente.api.donaciones.url}") String url,
      @Qualifier("tokenDelUsuario") ObjectProvider<ClientHttpRequestInterceptor> tokenDelUsuario) {
    return cliente(
        RestClient.builder(),
        url,
        DonacionesApi.class,
        tokenDelUsuario.stream().toArray(ClientHttpRequestInterceptor[]::new));
  }

  @Bean
  LogisticaApi logisticaApi(@Value("${cliente.api.logistica.url}") String url) {
    return cliente(RestClient.builder(), url, LogisticaApi.class);
  }

  @Bean
  NotificacionesApi notificacionesApi(@Value("${cliente.api.notificaciones.url}") String url) {
    return cliente(RestClient.builder(), url, NotificacionesApi.class);
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
