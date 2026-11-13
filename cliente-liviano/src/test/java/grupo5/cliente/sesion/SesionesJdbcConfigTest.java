package grupo5.cliente.sesion;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** La sesión en PostgreSQL solo se arma si se la pide: sin eso, el sitio no necesita base. */
class SesionesJdbcConfigTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(SesionesJdbcConfig.class);

  @Test
  void sinLaPropiedadNoArmaNadaYNoNecesitaBaseDeDatos() {
    runner.run(
        contexto -> {
          assertThat(contexto).hasNotFailed();
          assertThat(contexto).doesNotHaveBean(SesionesJdbcConfig.class);
          assertThat(contexto).doesNotHaveBean(DataSource.class);
        });
  }

  @Test
  void conLaPropiedadEnFalseTampocoArmaNada() {
    runner
        .withPropertyValues("cliente.sesiones.jdbc.habilitado=false")
        .run(
            contexto -> {
              assertThat(contexto).hasNotFailed();
              assertThat(contexto).doesNotHaveBean(SesionesJdbcConfig.class);
              assertThat(contexto).doesNotHaveBean(DataSource.class);
            });
  }
}
