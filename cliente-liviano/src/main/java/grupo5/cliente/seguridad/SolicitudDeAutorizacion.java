package grupo5.cliente.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/**
 * Le pasa a Keycloak los dos pedidos que el sitio puede hacer al enviar a alguien a ingresar: crear
 * una cuenta ({@code prompt=create}) y entrar directo con Google ({@code kc_idp_hint=google}).
 * Cualquier otro valor se ignora.
 */
final class SolicitudDeAutorizacion implements OAuth2AuthorizationRequestResolver {

  private static final Map<String, Set<String>> PERMITIDOS =
      Map.of("prompt", Set.of("create"), "kc_idp_hint", Set.of("google"));

  private final OAuth2AuthorizationRequestResolver base;

  SolicitudDeAutorizacion(ClientRegistrationRepository clientes) {
    this.base = new DefaultOAuth2AuthorizationRequestResolver(clientes, "/oauth2/authorization");
  }

  @Override
  public OAuth2AuthorizationRequest resolve(HttpServletRequest pedido) {
    return conPedidos(base.resolve(pedido), pedido);
  }

  @Override
  public OAuth2AuthorizationRequest resolve(HttpServletRequest pedido, String cliente) {
    return conPedidos(base.resolve(pedido, cliente), pedido);
  }

  private static OAuth2AuthorizationRequest conPedidos(
      OAuth2AuthorizationRequest solicitud, HttpServletRequest pedido) {
    if (solicitud == null) {
      return null;
    }
    Map<String, Object> parametros = new HashMap<>(solicitud.getAdditionalParameters());
    PERMITIDOS.forEach(
        (nombre, valores) -> {
          String valor = pedido.getParameter(nombre);
          if (valor != null && valores.contains(valor)) {
            parametros.put(nombre, valor);
          }
        });
    return OAuth2AuthorizationRequest.from(solicitud).additionalParameters(parametros).build();
  }
}
