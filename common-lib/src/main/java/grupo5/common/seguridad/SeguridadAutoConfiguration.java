package grupo5.common.seguridad;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Resource server de los servicios, activable con {@code donatrack.seguridad.habilitada=true}.
 * Apagada, la cadena deja pasar todo y {@link ActorActual} resuelve un {@link Administrador}.
 */
@AutoConfiguration(
    before = {
      ServletWebSecurityAutoConfiguration.class,
      OAuth2ResourceServerAutoConfiguration.class
    })
@ConditionalOnClass(BearerTokenAuthenticationFilter.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SeguridadAutoConfiguration {

  private static final String PROPIEDAD = "donatrack.seguridad.habilitada";

  @Bean
  @ConditionalOnBooleanProperty(name = PROPIEDAD, havingValue = false, matchIfMissing = true)
  SecurityFilterChain cadenaAbierta(HttpSecurity http) throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .anonymous(anonimo -> anonimo.principal(new Administrador()))
        .authorizeHttpRequests(rutas -> rutas.anyRequest().permitAll())
        .build();
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnBooleanProperty(PROPIEDAD)
  @EnableMethodSecurity
  @Import(RespuestasDeSeguridad.class)
  static class Habilitada {

    private final HandlerExceptionResolver errores;

    Habilitada(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver errores) {
      this.errores = errores;
    }

    @Bean
    ReglasDeAcceso rutasDeInfraestructura() {
      return () -> List.of("/actuator/health", "/swagger-ui/**", "/v3/api-docs/**");
    }

    @Bean
    SecurityFilterChain cadenaProtegida(HttpSecurity http, List<ReglasDeAcceso> reglas)
        throws Exception {
      return http.csrf(AbstractHttpConfigurer::disable)
          .sessionManagement(sesion -> sesion.sessionCreationPolicy(STATELESS))
          .authorizeHttpRequests(rutas -> rutas.requestMatchers(publicas(reglas)).permitAll())
          .authorizeHttpRequests(rutas -> rutas.anyRequest().authenticated())
          .oauth2ResourceServer(this::validarTokens)
          .build();
    }

    private void validarTokens(OAuth2ResourceServerConfigurer<HttpSecurity> servidor) {
      servidor.jwt(jwt -> jwt.jwtAuthenticationConverter(Actores::autenticacion));
      servidor.authenticationEntryPoint(this::responderError);
      servidor.accessDeniedHandler(this::responderError);
    }

    /** 401 y 403 salen por {@link RespuestasDeSeguridad}, igual que los errores de controller. */
    private void responderError(
        HttpServletRequest pedido, HttpServletResponse respuesta, RuntimeException ex) {
      errores.resolveException(pedido, respuesta, null, ex);
    }

    private static String[] publicas(List<ReglasDeAcceso> reglas) {
      return reglas.stream()
          .flatMap(regla -> regla.rutasPublicas().stream())
          .toArray(String[]::new);
    }
  }
}
