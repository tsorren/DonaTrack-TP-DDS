package grupo5.common.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

/** Los servicios escanean {@code grupo5}: sin Spring Security, el advice no se registra. */
class RespuestasDeSeguridadEscaneoTest {

  @Configuration(proxyBeanMethods = false)
  @ComponentScan(
      basePackageClasses = RespuestasDeSeguridad.class,
      useDefaultFilters = false,
      includeFilters =
          @ComponentScan.Filter(
              type = FilterType.ASSIGNABLE_TYPE,
              classes = RespuestasDeSeguridad.class))
  static class EscaneoComoUnServicio {}

  private final ApplicationContextRunner contexto =
      new ApplicationContextRunner().withUserConfiguration(EscaneoComoUnServicio.class);

  @Test
  void sinSpringSecurity_noRegistraElAdvice() {
    contexto
        .withClassLoader(new FilteredClassLoader("org.springframework.security"))
        .run(ctx -> assertThat(ctx).hasNotFailed().doesNotHaveBean(RespuestasDeSeguridad.class));
  }

  @Test
  void conSpringSecurity_registraElAdvice() {
    contexto.run(ctx -> assertThat(ctx).hasSingleBean(RespuestasDeSeguridad.class));
  }
}
