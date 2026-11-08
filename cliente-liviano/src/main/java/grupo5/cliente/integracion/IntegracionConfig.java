package grupo5.cliente.integracion;

import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

/**
 * Los clientes HTTP del sitio hacia los servicios y hacia la Admin API de Keycloak. Los dos van con
 * el token técnico del cliente liviano (registro {@code servicio}), que el interceptor pide y
 * renueva solo; un único token sirve para todas las sesiones.
 */
@Configuration
@Profile("!maqueta")
class IntegracionConfig {

  private static final String REGISTRO = "servicio";

  @Bean
  OAuth2ClientHttpRequestInterceptor tokenDeServicio(ClientRegistrationRepository clientes) {
    var manager =
        new AuthorizedClientServiceOAuth2AuthorizedClientManager(
            clientes, new InMemoryOAuth2AuthorizedClientService(clientes));
    var interceptor = new OAuth2ClientHttpRequestInterceptor(manager);
    var proceso =
        new AnonymousAuthenticationToken(REGISTRO, REGISTRO, createAuthorityList("ROLE_ANONYMOUS"));
    interceptor.setClientRegistrationIdResolver(pedido -> REGISTRO);
    interceptor.setPrincipalResolver(pedido -> proceso);
    return interceptor;
  }

  @Bean
  RestClient donaciones(
      OAuth2ClientHttpRequestInterceptor tokenDeServicio,
      @Value("${donatrack.donaciones.url}") String url) {
    return conToken(url, tokenDeServicio);
  }

  @Bean
  RestClient keycloak(
      OAuth2ClientHttpRequestInterceptor tokenDeServicio,
      @Value("${spring.security.oauth2.client.provider.keycloak.issuer-uri}") String emisor) {
    return conToken(emisor.replace("/realms/", "/admin/realms/"), tokenDeServicio);
  }

  private static RestClient conToken(String url, OAuth2ClientHttpRequestInterceptor token) {
    return RestClient.builder().baseUrl(url).requestInterceptor(token).build();
  }
}
