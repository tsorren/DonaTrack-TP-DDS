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
 * La lámina, la etiqueta de una entrega y la trama de la guía (ola C1, lote c1-entrega, ítems C1-17
 * a C1-19): la tarjeta sin la sombra granulada y con sus tonos (la protagonista con la sombra de
 * paja), la etiqueta-entrega con la foto arriba o al costado y la trama, cada una con su ficha en
 * /estilos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("maqueta")
class EstilosEntregaTest {

  private static final Path CSS = Path.of("src/main/resources/static/css");

  @Autowired private MockMvc mvc;

  private String estilos() throws Exception {
    return mvc.perform(get("/estilos"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString(StandardCharsets.UTF_8);
  }

  @Test
  void laTarjetaEsLaLaminaDeLaGuia() throws Exception {
    String hoja = leer(CSS.resolve("componentes/tarjeta.css"));
    assertThat(hoja)
        .doesNotContain("grano-sombra")
        .contains("var(--borde-fino)")
        .contains("var(--sombra-paja)")
        .contains("var(--borde-punteado)")
        .contains("var(--color-pendiente-fondo)");
    assertThat(estilos())
        .contains(
            "<code>.tarjeta</code>",
            "data-papel=\"protagonista\"",
            "data-papel=\"hueco\"",
            "data-papel=\"tierra\"",
            "data-filete=\"contorno\"",
            "class=\"tarjeta-cabeza\"",
            "<a class=\"tarjeta");
  }

  @Test
  void laEtiquetaDeUnaEntregaTraeLaFotoArribaYAlCostado() throws Exception {
    String html = estilos();
    assertThat(html)
        .contains(
            "<code>.etiqueta-entrega</code>",
            "class=\"etiqueta-entrega\"",
            "class=\"etiqueta-entrega etiqueta-entrega--costado\"",
            "class=\"etiqueta-entrega-foto\"",
            "class=\"etiqueta-entrega-rotulo\"",
            "class=\"etiqueta-entrega-cuerpo\"",
            "class=\"etiqueta-entrega-pie\"",
            "Foto de ejemplo")
        .doesNotContain("class=\"etiqueta\"", "class=\"foto\"");
    assertThat(leer(CSS.resolve("app.css")))
        .contains("@import url(\"componentes/etiqueta-entrega.css\") layer(bloques);");
  }

  @Test
  void laTramaEsSoloUnFondo() throws Exception {
    assertThat(estilos())
        .contains("<code>.trama</code>", "class=\"marco trama\"", "class=\"tarjeta trama pila\"");
    String hoja = leer(CSS.resolve("componentes/trama.css"));
    assertThat(hoja).contains("background-image:").contains("var(--color-trama)");
    assertThat(leer(CSS.resolve("app.css")))
        .contains("@import url(\"componentes/trama.css\") layer(bloques);");
  }

  private static String leer(Path archivo) throws IOException {
    return Files.readString(archivo, StandardCharsets.UTF_8);
  }
}
