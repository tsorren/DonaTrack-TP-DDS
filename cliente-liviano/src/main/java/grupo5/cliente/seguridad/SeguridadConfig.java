package grupo5.cliente.seguridad;

import static org.springframework.http.HttpMethod.POST;

import grupo5.cliente.puertos.CuentasDelMenu;
import grupo5.cliente.vistas.CuentaVista;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer.AuthorizationEndpointConfig;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

/** Ingreso con Keycloak (authorization code) y acceso por área. Fuera del perfil maqueta. */
@Configuration
@Profile("!maqueta")
class SeguridadConfig {

  private static final String[] PUBLICAS = {
    "/",
    "/publico-*",
    "/error",
    "/actuator/health",
    "/css/**",
    "/js/**",
    "/img/**",
    "/fuentes/**",
    "/webjars/**"
  };

  private final ClientRegistrationRepository clientes;

  SeguridadConfig(ClientRegistrationRepository clientes) {
    this.clientes = clientes;
  }

  @Bean
  SecurityFilterChain cadena(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
        rutas -> {
          rutas.requestMatchers(POST, "/publico-registro-*").authenticated();
          rutas.requestMatchers(PUBLICAS).permitAll();
          for (Area area : Area.values()) {
            rutas.requestMatchers("/" + area.prefijo() + "-*").hasRole(area.name());
          }
          rutas.anyRequest().authenticated();
        });
    http.addFilterBefore(new PerfilPendiente(), AuthorizationFilter.class);
    http.oauth2Login(this::ingresarConKeycloak);
    http.logout(this::cerrarSesionEnKeycloak);
    return http.build();
  }

  private void ingresarConKeycloak(OAuth2LoginConfigurer<HttpSecurity> login) {
    login.authorizationEndpoint(this::pedirAKeycloak).defaultSuccessUrl("/inicio");
  }

  private void pedirAKeycloak(AuthorizationEndpointConfig endpoint) {
    endpoint.authorizationRequestResolver(new SolicitudDeAutorizacion(clientes));
  }

  /** Cierra también la sesión de Keycloak y vuelve a la portada. */
  private void cerrarSesionEnKeycloak(LogoutConfigurer<HttpSecurity> salida) {
    OidcClientInitiatedLogoutSuccessHandler cierre =
        new OidcClientInitiatedLogoutSuccessHandler(clientes);
    cierre.setPostLogoutRedirectUri("{baseUrl}/");
    salida.logoutSuccessHandler(cierre);
  }

  /** Los roles y permisos del ID token pasan a ser las autoridades de la sesión. */
  @Bean
  static GrantedAuthoritiesMapper autoridades() {
    return autoridades ->
        autoridades.stream()
            .filter(OidcUserAuthority.class::isInstance)
            .map(OidcUserAuthority.class::cast)
            .flatMap(a -> UsuarioActual.desde(a.getAttributes()).autoridades().stream())
            .toList();
  }

  /**
   * El ID token no trae nombre (RNF-AUTH-09): hasta que Donaciones lo aporte, el menú dice el área.
   * El cambio de contraseña es de la consola de cuenta de Keycloak.
   */
  @Bean
  CuentasDelMenu cuentasDelMenu(
      @Value("${spring.security.oauth2.client.provider.keycloak.issuer-uri}") String emisor) {
    return prefijo ->
        Area.deRuta(prefijo)
            .map(area -> new CuentaVista(area.etiqueta(), "Sesión iniciada", emisor + "/account"));
  }
}
