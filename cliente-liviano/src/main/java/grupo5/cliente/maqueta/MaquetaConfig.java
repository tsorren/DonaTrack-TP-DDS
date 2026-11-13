package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.AvisosSinLeer;
import grupo5.cliente.puertos.CuentasDelMenu;
import grupo5.cliente.seguridad.Area;
import grupo5.cliente.vistas.CuentaVista;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Perfil {@code maqueta}: datos de ejemplo, escenarios y variantes A/B para diseñar. Todo el sitio
 * queda abierto, sin ingreso, y "Cerrar sesión" vuelve a la portada.
 */
@Configuration
@Profile("maqueta")
public class MaquetaConfig implements WebMvcConfigurer {

  /** La cuenta de ejemplo del donante es una empresa. */
  private static final Map<Area, CuentaVista> CUENTAS =
      Map.of(
          Area.DONANTE, new CuentaVista("Distribuidora Ejemplo SA", "Empresa donante"),
          Area.ENTIDAD, new CuentaVista("Comedor Barrio Ejemplo", "Organización"),
          Area.ADMIN, new CuentaVista("Leo", "Administración"));

  private static final Map<String, Long> SIN_LEER =
      Map.of("donante", 4L, "entidad", 3L, "admin", 3L);

  private final ResourceLoader recursos;

  MaquetaConfig(ResourceLoader recursos) {
    this.recursos = recursos;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(
        new VariantesInterceptor(ResourcePatternUtils.getResourcePatternResolver(recursos)));
    registry.addInterceptor(new RespuestaDeEjemplo());
    registry.addInterceptor(new CuentaDeEjemplo());
  }

  @Bean
  SecurityFilterChain cadenaAbierta(HttpSecurity http) throws Exception {
    return http.authorizeHttpRequests(rutas -> rutas.anyRequest().permitAll())
        .csrf(AbstractHttpConfigurer::disable)
        .logout(salida -> salida.logoutSuccessUrl("/"))
        .build();
  }

  @Bean
  CuentasDelMenu cuentasDelMenu() {
    return prefijo -> Area.deRuta(prefijo).map(CUENTAS::get);
  }

  /**
   * Las sin leer de la cuenta de ejemplo de cada área, las de su bandeja de ejemplo (la persona que
   * dona tiene las suyas: las pone {@link CuentaDeEjemplo}).
   */
  @Bean
  @ConditionalOnProperty(
      name = "cliente.datos.avisos",
      havingValue = "maqueta",
      matchIfMissing = true)
  AvisosSinLeer avisosSinLeer() {
    return (prefijo, pedido) -> SIN_LEER.getOrDefault(prefijo, 0L);
  }
}
