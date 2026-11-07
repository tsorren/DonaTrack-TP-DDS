package grupo5.cliente.rest;

import grupo5.cliente.web.TraceIdFiltro;
import java.time.Clock;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Un {@link RestClient} por API, con su URL base ({@code cliente.api.<api>.url}). Cada llamada
 * lleva el {@code X-Trace-Id} del pedido del navegador (ADR-E6-02: lo genera el cliente). Sin login
 * todavía, no lleva token (F4 suma el {@code Authorization} en este mismo interceptor).
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
  DonacionesApi donacionesApi(@Value("${cliente.api.donaciones.url}") String url) {
    return cliente(RestClient.builder(), url, DonacionesApi.class);
  }

  /** Arma el proxy de una API; los tests le pasan el builder atado a MockRestServiceServer. */
  static <T> T cliente(RestClient.Builder builder, String url, Class<T> api) {
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
            .build();
    return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
        .build()
        .createClient(api);
  }
}
