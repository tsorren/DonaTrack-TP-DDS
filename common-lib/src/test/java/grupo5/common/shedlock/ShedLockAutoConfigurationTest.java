package grupo5.common.shedlock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.scheduling.annotation.EnableScheduling;

class ShedLockAutoConfigurationTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(ShedLockAutoConfiguration.class));

  @Test
  void conBaseYTareasProgramadas_creaElLockProviderJdbc() {
    runner
        .withUserConfiguration(ConBase.class, ConTareas.class)
        .run(
            contexto ->
                assertThat(contexto)
                    .hasSingleBean(LockProvider.class)
                    .getBean(LockProvider.class)
                    .isInstanceOf(JdbcTemplateLockProvider.class));
  }

  @Test
  void sinBase_noCreaNada() {
    runner
        .withUserConfiguration(ConTareas.class)
        .run(
            contexto ->
                assertThat(contexto)
                    .hasNotFailed()
                    .doesNotHaveBean(LockProvider.class)
                    .doesNotHaveBean(ShedLockAutoConfiguration.class));
  }

  @Test
  void conBaseSinTareasProgramadas_noCreaNada() {
    runner
        .withUserConfiguration(ConBase.class)
        .run(
            contexto ->
                assertThat(contexto)
                    .hasNotFailed()
                    .doesNotHaveBean(LockProvider.class)
                    .doesNotHaveBean(ShedLockAutoConfiguration.class));
  }

  @Test
  void laTablaVaEnElEsquemaDeFlyway() {
    assertThat(ShedLockAutoConfiguration.tabla(new MockEnvironment())).isEqualTo("shedlock");
    assertThat(
            ShedLockAutoConfiguration.tabla(
                new MockEnvironment().withProperty("spring.flyway.schemas", "donaciones,otro")))
        .isEqualTo("donaciones.shedlock");
    assertThat(
            ShedLockAutoConfiguration.tabla(
                new MockEnvironment()
                    .withProperty("spring.flyway.schemas", "logistica")
                    .withProperty("spring.flyway.default-schema", "propio")))
        .isEqualTo("propio.shedlock");
  }

  @Configuration(proxyBeanMethods = false)
  static class ConBase {
    @Bean
    DataSource dataSource() {
      return mock(DataSource.class);
    }
  }

  @Configuration(proxyBeanMethods = false)
  @EnableScheduling
  static class ConTareas {}
}
