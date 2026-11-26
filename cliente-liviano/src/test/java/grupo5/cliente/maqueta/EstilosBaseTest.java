package grupo5.cliente.maqueta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * La base, el movimiento y las fichas de tokens de la guía «Huerta y galpón» (ola C1, lote c1-base,
 * ítems C1-01 a C1-04): el aro de foco de un solo color, la regla de movimiento reducido, las
 * animaciones con nombre que piden los otros lotes y las 14 fichas de tokens armadas con el
 * fragmento {@code componentes/muestra :: muestra}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("maqueta")
class EstilosBaseTest {

  private static final Path CSS = Path.of("src/main/resources/static/css");
  private static final String FICHA_DE_TOKENS = "<ul class=\"muestra-tokens\"";

  @Autowired private MockMvc mvc;

  @Test
  void lasCatorceFichasDeTokensSonElFragmentoMuestra() throws Exception {
    String pagina =
        mvc.perform(get("/estilos"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);

    String[] partes = pagina.split(FICHA_DE_TOKENS, -1);
    assertThat(partes).hasSize(15);
    assertThat(pagina).doesNotContain("muestra-tokens muestra-demo");
    assertThat(pagina)
        .contains("<code>Color: superficies y texto</code>")
        .contains("<code>Letra</code>")
        .contains("<code>Movimiento</code>");
    for (int i = 0; i < partes.length - 1; i++) {
      assertThat(partes[i].stripTrailing())
          .as("la ficha %d es el cuerpo de .muestra-demo", i + 1)
          .endsWith("<div class=\"muestra-demo\">");
    }
  }

  @Test
  void elAroDeFocoEsDeUnSoloColor() throws IOException {
    String app = leer(CSS.resolve("app.css"));
    assertThat(app)
        .contains("outline: var(--borde-ancho-foco) solid var(--color-foco);")
        .contains("box-shadow: 0 0 0 2px var(--color-foco-separador);")
        .contains("outline-color: var(--color-foco-sobre-suelo);")
        .doesNotContain("outline: 3px solid var(--color-acento)");
  }

  @Test
  void movimientoTraeLaReglaReducidaYLasAnimacionesConNombre() throws IOException {
    String movimiento = leer(CSS.resolve("movimiento.css"));
    assertThat(movimiento)
        .contains("@media (prefers-reduced-motion: reduce)")
        .contains("animation: none !important;")
        .contains("transition: none !important;")
        .contains("--movimiento-animacion-gira: mov-gira var(--movimiento-duracion-larga)")
        .contains("--movimiento-animacion-late: mov-late var(--movimiento-duracion-trazo)")
        .contains("--movimiento-animacion-late-segundo:")
        .contains("--movimiento-animacion-late-tercero:")
        .contains("--movimiento-animacion-traza-raiz: mov-traza var(--movimiento-duracion-trazo)")
        .contains("--movimiento-animacion-asoma: mov-aparece")
        .contains("--movimiento-animacion-brote: mov-brote")
        .contains("@keyframes mov-gira")
        .contains("@keyframes mov-late")
        .contains("@keyframes mov-brote")
        .doesNotContain("dt-");
  }

  @Test
  void elCatalogoYaNoNombraAPicoComoPrimeraCapa() throws IOException {
    String catalogo = leer(Path.of("../docs/interfaz/componentes.md"));
    assertThat(catalogo)
        .doesNotContain("`pico, tokens")
        .doesNotContain("capa `pico`")
        .contains("`base, tokens, composicion, utilidades, bloques,");
  }

  private static String leer(Path archivo) throws IOException {
    return Files.readString(archivo, StandardCharsets.UTF_8);
  }
}
