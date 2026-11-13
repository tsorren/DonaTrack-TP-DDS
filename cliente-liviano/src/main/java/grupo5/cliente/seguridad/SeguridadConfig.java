package grupo5.cliente.seguridad;

import static org.springframework.http.HttpMethod.POST;

import grupo5.cliente.puertos.CuentasDelMenu;
import grupo5.cliente.puertos.DirectorioDeAdministradores;
import grupo5.cliente.puertos.DonantePerfilPuerto;
import grupo5.cliente.puertos.EntidadPerfilPuerto;
import org.springframework.beans.factory.ObjectProvider;
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
   * La cuenta del menú y del saludo: su nombre, pedido una vez por sesión (R11-44; el ID token no
   * lo trae, RNF-AUTH-09). El cambio de contraseña es de la consola de cuenta de Keycloak.
   */
  @Bean
  CuentasDelMenu cuentasDelMenu(
      @Value("${spring.security.oauth2.client.provider.keycloak.issuer-uri}") String emisor,
      ObjectProvider<DonantePerfilPuerto> donantes,
      ObjectProvider<EntidadPerfilPuerto> entidades,
      ObjectProvider<DirectorioDeAdministradores> administradores) {
    return new CuentaDeLaSesion(emisor, donantes, entidades, administradores);
  }
}
