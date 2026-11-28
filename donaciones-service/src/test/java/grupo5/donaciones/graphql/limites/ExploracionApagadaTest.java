package grupo5.donaciones.graphql.limites;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.donaciones.graphql.comun.BaseSliceGraphQl;
import grupo5.donaciones.graphql.entidades.EntidadesResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;

/**
 * Con el perfil por defecto la introspección y GraphiQL están apagados (SPEC-11, G3; contrato de
 * GraphQL, §4.7). La seguridad se apaga en este test para que {@code /graphiql} responda 404 y no
 * 401: sin la cadena abierta, una ruta que no existe igual pide token.
 */
@WebMvcTest(controllers = EntidadesResolver.class)
@TestPropertySource(properties = "donatrack.seguridad.habilitada=false")
class ExploracionApagadaTest extends BaseSliceGraphQl {

  @Autowired private Environment entorno;

  @Test
  void laIntrospeccionEstaApagada() throws Exception {
    consultar("{ __schema { queryType { name } } }", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.__schema").doesNotExist())
        .andExpect(jsonPath("$.errors[0].message").exists());
    assertThat(entorno.getProperty("spring.graphql.schema.introspection.enabled"))
        .isEqualTo("false");
  }

  @Test
  void graphiqlEstaApagado() throws Exception {
    mockMvc.perform(get("/graphiql")).andExpect(status().isNotFound());
    assertThat(entorno.getProperty("spring.graphql.graphiql.enabled")).isEqualTo("false");
  }
}
