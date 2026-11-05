package grupo5.cliente.maqueta;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Perfil {@code maqueta}: datos de ejemplo, escenarios y variantes A/B para diseñar. */
@Configuration
@Profile("maqueta")
class MaquetaConfig implements WebMvcConfigurer {

  private final ResourceLoader recursos;

  MaquetaConfig(ResourceLoader recursos) {
    this.recursos = recursos;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(
        new VariantesInterceptor(ResourcePatternUtils.getResourcePatternResolver(recursos)));
  }

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    // ponytail: el inicio lleva al catálogo hasta que exista la primera pantalla real.
    registry.addRedirectViewController("/", "/estilos");
  }
}
