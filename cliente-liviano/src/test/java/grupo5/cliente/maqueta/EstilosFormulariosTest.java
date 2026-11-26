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
 * Los botones, los campos, la opción, la casilla y la zona para subir fotos de la guía «Huerta y
 * galpón» (ola C1, lote c1-formularios, ítems C1-11 a C1-13): el botón con su estado «cargando»,
 * los controles con el borde de control y el error en el color del problema, la opción como bloque
 * que se toca entero y la zona punteada con su botón de elegir.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("maqueta")
class EstilosFormulariosTest {

  private static final Path CSS = Path.of("src/main/resources/static/css");

  @Autowired private MockMvc mvc;

  @Test
  void elBotonEsDeLaGuiaYCargaConElGiroDeMovimiento() throws IOException {
    String boton = leer(CSS.resolve("componentes/boton.css"));
    assertThat(boton)
        .contains("border: var(--borde-tinta);")
        .contains("background: var(--color-accion-secundaria);")
        .contains("background: var(--color-accion-primaria);")
        .contains("color: var(--color-error-texto);")
        .contains(".boton[aria-busy=\"true\"]::before")
        .contains("animation: var(--movimiento-animacion-gira);")
        .contains(".boton:is(:disabled, [aria-disabled=\"true\"])")
        .contains("background: var(--color-deshabilitado-fondo);")
        .doesNotContain("box-shadow: inset 0 0 0 2px var(--color-tinta)")
        .doesNotContain("@keyframes");
  }

  @Test
  void losCamposLaOpcionYLaCasillaSonDeLaGuia() throws IOException {
    String campo = leer(CSS.resolve("componentes/campo.css"));
    assertThat(campo)
        .contains("border: var(--borde-control);")
        .contains("border-radius: var(--radio-campo);")
        .contains("[aria-invalid=\"true\"]")
        .contains(":disabled")
        .contains("background-color: var(--color-deshabilitado-fondo);")
        .doesNotContain("box-shadow: inset 0 1px 2px");
    String formularios = leer(CSS.resolve("formularios.css"));
    assertThat(formularios)
        .contains(".opciones label:has(:checked)")
        .contains("background: var(--color-salvia);")
        .contains(".opciones label:has([type=\"checkbox\"])::before")
        .contains(".opciones label:has(:disabled)")
        .contains(".opciones label:has([aria-invalid=\"true\"])");
  }

  @Test
  void laZonaDeSubirFotosEsPunteadaYSuBotonEsElDeLaGuia() throws IOException {
    String subida = leer(CSS.resolve("componentes/subida.css"));
    assertThat(subida)
        .contains("border: var(--borde-punteado);")
        .contains("::file-selector-button")
        .contains("min-height: var(--medida-toque-minimo);")
        .contains("background: var(--color-accion-secundaria);");
  }

  @Test
  void estilosMuestraLosEstadosDeCadaPiezaYLasFichasNuevas() throws Exception {
    String pagina =
        mvc.perform(get("/estilos"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    assertThat(pagina)
        .contains("aria-busy=\"true\"")
        .contains("<code>.opciones</code>")
        .contains("<code>.opciones con casillas</code>")
        .contains("type=\"checkbox\"")
        .contains("data-tono=\"quitar\" disabled");
    assertThat(pagina.indexOf("<code>.opciones</code>"))
        .as("la ficha de la opción va justo después de la de .campo")
        .isGreaterThan(pagina.indexOf("<code>.campo</code>"))
        .isLessThan(pagina.indexOf("<code>.opciones con casillas</code>"));
  }

  private static String leer(Path archivo) throws IOException {
    return Files.readString(archivo, StandardCharsets.UTF_8);
  }
}
