package grupo5.donaciones.graphql.limites;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.time.Duration;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

/**
 * Los valores de los límites en {@code application.properties} (SPEC-11, G3): la profundidad deja
 * pasar la consulta legítima más profunda (5 niveles, contrato de GraphQL §4.5), el tiempo máximo
 * queda por debajo de la espera de lectura del gateway (10 s) y la exploración arranca apagada.
 */
class ValoresDeLosLimitesTest {

  /** La espera de lectura del gateway hacia los servicios (SPEC-11, §7.1). */
  private static final Duration ESPERA_DEL_GATEWAY = Duration.ofSeconds(10);

  /** {@code donaciones → elementos → necesidad → entidad → razonSocial}. */
  private static final int CONSULTA_LEGITIMA_MAS_PROFUNDA = 5;

  private final Properties propiedades = cargar();

  @Test
  void laProfundidadMaximaDejaPasarLaConsultaLegitimaMasProfunda() {
    assertThat(entero("donatrack.graphql.limites.profundidad-maxima"))
        .isEqualTo(6)
        .isGreaterThanOrEqualTo(CONSULTA_LEGITIMA_MAS_PROFUNDA);
  }

  @Test
  void laComplejidadMaximaEsCienCampos() {
    assertThat(entero("donatrack.graphql.limites.complejidad-maxima")).isEqualTo(100);
  }

  @Test
  void lasRaicesMaximasSonCinco() {
    assertThat(entero("donatrack.graphql.limites.raices-maximas")).isEqualTo(5);
  }

  @Test
  void elTiempoMaximoQuedaPorDebajoDeLaEsperaDelGateway() {
    Duration tiempo = Duration.ofMillis(entero("donatrack.graphql.limites.tiempo-maximo-ms"));

    assertThat(tiempo).isEqualTo(Duration.ofSeconds(5)).isLessThan(ESPERA_DEL_GATEWAY);
  }

  @Test
  void laIntrospeccionYGraphiqlArrancanApagadosYLosEnciendeUnaSolaVariable() {
    assertThat(propiedades.getProperty("spring.graphql.schema.introspection.enabled"))
        .isEqualTo("${DONACIONES_GRAPHQL_EXPLORAR:false}");
    assertThat(propiedades.getProperty("spring.graphql.graphiql.enabled"))
        .isEqualTo("${DONACIONES_GRAPHQL_EXPLORAR:false}");
  }

  @Test
  void unTopeMenorQueUnoNoArranca() {
    RechazosDeConsulta rechazos = new RechazosDeConsulta(new SimpleMeterRegistry());

    assertThatThrownBy(() -> new LimitesDeConsulta(0, 100, 5, rechazos))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new LimitesDeConsulta(6, 0, 5, rechazos))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new LimitesDeConsulta(6, 100, 0, rechazos))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private int entero(String clave) {
    return Integer.parseInt(propiedades.getProperty(clave));
  }

  private static Properties cargar() {
    try {
      return PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"));
    } catch (IOException e) {
      throw new IllegalStateException("No se pudo leer application.properties", e);
    }
  }
}
