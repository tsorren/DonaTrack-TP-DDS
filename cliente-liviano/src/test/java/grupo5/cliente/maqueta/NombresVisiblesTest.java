package grupo5.cliente.maqueta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * R11-44 y R11-45 en las pantallas: el saludo y el menú dicen el nombre de la cuenta, y quien
 * recibe se llama "Organización" en todo lo que se ve. Las rutas, las clases y los atributos siguen
 * diciendo "entidad": solo se mira el texto, sin las etiquetas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("maqueta")
class NombresVisiblesTest {

  @Autowired private MockMvc mvc;

  private String textoVisible(String ruta) throws Exception {
    String html =
        mvc.perform(get(ruta))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    return html.replaceAll("(?s)<script.*?</script>", " ")
        .replaceAll("(?s)<style.*?</style>", " ")
        .replaceAll("(?s)<[^>]*>", " ");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/",
        "/publico-entidades",
        "/publico-mapa",
        "/publico-para-entidades",
        "/publico-registro",
        "/publico-registro-entidad",
        "/donante-inicio",
        "/donante-entidades",
        "/donante-perfil",
        "/entidad-inicio",
        "/entidad-perfil",
        "/admin-inicio",
        "/admin-entidades",
        "/admin-donantes",
        "/admin-entidades-registrar",
        "/admin-donantes-registrar?tipo=organizacion"
      })
  void quienRecibeSeLlamaOrganizacion(String ruta) throws Exception {
    assertThat(textoVisible(ruta)).doesNotContainIgnoringCase("entidad");
  }

  @ParameterizedTest
  @ValueSource(strings = {"/donante-inicio", "/admin-donantes", "/publico-registro"})
  void elDonanteJuridicoNoSeLlamaOrganizacion(String ruta) throws Exception {
    assertThat(textoVisible(ruta))
        .doesNotContain("organización donante")
        .doesNotContain("Organización donante");
  }

  @ParameterizedTest
  @ValueSource(strings = {"/donante-inicio", "/entidad-inicio", "/admin-inicio"})
  void elSaludoDiceElNombreDeLaCuentaYNoElRol(String ruta) throws Exception {
    assertThat(textoVisible(ruta))
        .doesNotContain("Hola, Donante")
        .doesNotContain("Hola, Entidad")
        .doesNotContain("Hola, Organización")
        .doesNotContain("Hola, Administración");
  }
}
