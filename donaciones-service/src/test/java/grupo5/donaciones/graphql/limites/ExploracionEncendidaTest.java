package grupo5.donaciones.graphql.limites;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import grupo5.donaciones.graphql.comun.BaseSliceGraphQl;
import grupo5.donaciones.graphql.entidades.EntidadesResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;

/**
 * La variable {@code DONACIONES_GRAPHQL_EXPLORAR=true}, la que pone {@code
 * docker-compose.preprod.yml}, enciende GraphiQL y la introspección (contrato de GraphQL, §4.7).
 *
 * <p>De la introspección se comprueba la propiedad y no una consulta: Spring Boot la apaga para
 * toda la JVM ({@code Introspection.enabledJvmWide(false)}) cuando un contexto la tiene apagada, y
 * nunca la vuelve a encender, así que el resultado dependería del orden de los tests.
 */
@WebMvcTest(controllers = EntidadesResolver.class)
@TestPropertySource(
    properties = {"donatrack.seguridad.habilitada=false", "DONACIONES_GRAPHQL_EXPLORAR=true"})
class ExploracionEncendidaTest extends BaseSliceGraphQl {

  @Autowired private Environment entorno;

  @Test
  void laVariableEnciendeLaIntrospeccionYGraphiql() throws Exception {
    assertThat(entorno.getProperty("spring.graphql.schema.introspection.enabled"))
        .isEqualTo("true");
    assertThat(entorno.getProperty("spring.graphql.graphiql.enabled")).isEqualTo("true");

    int estado = mockMvc.perform(get("/graphiql")).andReturn().getResponse().getStatus();
    assertThat(estado).isNotEqualTo(404).isLessThan(400);
  }
}
