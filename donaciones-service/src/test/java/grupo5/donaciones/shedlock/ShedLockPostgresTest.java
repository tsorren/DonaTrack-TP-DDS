package grupo5.donaciones.shedlock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import grupo5.common.shedlock.ShedLockAutoConfiguration;
import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.donaciones.schedulers.VencimientoDonacionesScheduler;
import grupo5.donaciones.services.IDonacionesIndependientesService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Dos réplicas que disparan a la vez la misma tarea programada: con la tabla {@code shedlock} de la
 * migración y la autoconfiguración de common-lib, la tarea corre una sola vez.
 */
@Testcontainers
@DisabledIfDockerUnavailable
class ShedLockPostgresTest {

  private static final String ESQUEMA = "donaciones";

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @BeforeAll
  static void migrar() {
    Flyway.configure()
        .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
        .schemas(ESQUEMA)
        .locations("classpath:db/migration")
        .load()
        .migrate();
  }

  @Test
  void dosEjecucionesSimultaneasDelMismoLock_correnUnaSolaVez() {
    IDonacionesIndependientesService servicio = mock(IDonacionesIndependientesService.class);
    CountDownLatch dentro = new CountDownLatch(1);
    CountDownLatch soltar = new CountDownLatch(1);
    doAnswer(
            invocacion -> {
              dentro.countDown();
              assertThat(soltar.await(30, TimeUnit.SECONDS)).isTrue();
              return null;
            })
        .when(servicio)
        .vencer();

    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ShedLockAutoConfiguration.class))
        .withUserConfiguration(Base.class)
        .withBean(IDonacionesIndependientesService.class, () -> servicio)
        .withBean(VencimientoDonacionesScheduler.class)
        .run(
            contexto -> {
              VencimientoDonacionesScheduler tarea =
                  contexto.getBean(VencimientoDonacionesScheduler.class);

              CompletableFuture<Void> primera =
                  CompletableFuture.runAsync(tarea::verificarVencimiento);
              assertThat(dentro.await(30, TimeUnit.SECONDS)).isTrue();

              // La segunda llega mientras la primera tiene el lock: no corre y vuelve enseguida.
              CompletableFuture.runAsync(tarea::verificarVencimiento).get(30, TimeUnit.SECONDS);
              soltar.countDown();
              primera.get(30, TimeUnit.SECONDS);

              verify(servicio, times(1)).vencer();
              Integer filas =
                  contexto
                      .getBean(JdbcTemplate.class)
                      .queryForObject(
                          "SELECT count(*) FROM shedlock WHERE name = ?",
                          Integer.class,
                          "donaciones.vencimiento-donaciones");
              assertThat(filas).isEqualTo(1);
            });
  }

  @Configuration(proxyBeanMethods = false)
  @EnableScheduling
  static class Base {
    @Bean
    DataSource dataSource() {
      DriverManagerDataSource dataSource =
          new DriverManagerDataSource(
              postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
      dataSource.setSchema(ESQUEMA);
      return dataSource;
    }

    @Bean
    JdbcTemplate jdbcTemplate(DataSource dataSource) {
      return new JdbcTemplate(dataSource);
    }
  }
}
