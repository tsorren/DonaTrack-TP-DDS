package grupo5.cliente.visual;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Los rótulos de Bajo tierra, en la portada, no se pisan (docs/interfaz/flujos/r11-p1-portada.md,
 * sección 7, R11-17): desde 600 px, la caja de cada rótulo (con su contorno) no se cruza con
 * ninguna otra ni se sale del dibujo, sin tolerancia. Se mide con la letra de la página, con una
 * letra de respaldo más ancha, con "Tu organización" y con cada rótulo un 40 % más largo (el texto
 * se cambia solo en la medición). En 360 px, la lista apilada con los textos largos no se sale del
 * ancho. Corre con {@code -Pvisual}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("maqueta")
class BajoTierraRotulosTest {

  private static final List<Integer> ANCHOS = List.of(600, 700, 768, 900, 1024, 1280, 1440, 1920);

  private static final String ROTULOS =
      ".huerta-bajo :is(.huerta-bajo-categoria, .huerta-bajo-entidad, .huerta-bajo-tu)";

  /**
   * Cambia los textos para la medición: {@code largos} suma un 40 % de letras en palabras nuevas.
   */
  private static final String ALARGAR =
      """
      ([selector, largos, letra]) => {
        const rotulos = [...document.querySelectorAll(selector)];
        const tu = document.querySelector('.huerta-bajo-tu a');
        tu.firstChild.textContent = 'Tu organización';
        if (largos) {
          for (const rotulo of rotulos) {
            const nodo = rotulo.matches('.huerta-bajo-tu') ? tu.firstChild : rotulo.firstChild;
            const extra = Math.ceil(nodo.textContent.length * 0.4);
            const palabras = [];
            for (let resto = extra; resto > 0; resto -= 6) {
              palabras.push('mmmmmm'.slice(0, Math.min(6, resto)));
            }
            nodo.textContent = nodo.textContent + ' ' + palabras.join(' ');
          }
        }
        if (letra) {
          for (const rotulo of rotulos) rotulo.style.fontFamily = letra;
        }
      }
      """;

  /** Los pares de rótulos que se cruzan y los que se salen del dibujo; vacío si ninguno. */
  private static final String CRUCES =
      """
      selector => {
        const dibujo = document.querySelector('.huerta-bajo').getBoundingClientRect();
        const cajas = [...document.querySelectorAll(selector)].map(rotulo => {
          const caja = rotulo.getBoundingClientRect();
          return { nombre: rotulo.textContent.trim().slice(0, 30), caja };
        });
        const fallas = [];
        cajas.forEach(({ nombre, caja }, i) => {
          if (caja.left < dibujo.left || caja.right > dibujo.right) {
            fallas.push(nombre + ' se sale del dibujo');
          }
          cajas.slice(i + 1).forEach(otro => {
            const b = otro.caja;
            if (caja.left < b.right && b.left < caja.right
                && caja.top < b.bottom && b.top < caja.bottom) {
              fallas.push(nombre + ' pisa a ' + otro.nombre);
            }
          });
        });
        return fallas;
      }
      """;

  private static final String DESBORDA =
      "() => document.documentElement.scrollWidth > document.documentElement.clientWidth";

  @LocalServerPort private int puerto;

  @Test
  void losRotulosNoSePisanEnNingunAnchoNiConTextosLargos() {
    List<String> fallas = new ArrayList<>();
    try (Playwright playwright = Playwright.create();
        Browser navegador = playwright.chromium().launch()) {
      for (int ancho : ANCHOS) {
        medir(navegador, ancho, false, null, fallas);
        medir(navegador, ancho, true, null, fallas);
        medir(navegador, ancho, false, "Verdana, sans-serif", fallas);
        medir(navegador, ancho, true, "Verdana, sans-serif", fallas);
      }
    }
    assertThat(fallas).isEmpty();
  }

  @Test
  void enElCelularLaListaApiladaConTextosLargosNoSeSaleDelAncho() {
    try (Playwright playwright = Playwright.create();
        Browser navegador = playwright.chromium().launch()) {
      Page pagina = abrir(navegador, AnchosDeReferencia.CELULAR);
      pagina.evaluate(ALARGAR, List.of(ROTULOS, true, ""));
      assertThat(pagina.evaluate(DESBORDA)).isEqualTo(false);
      pagina.close();
    }
  }

  private void medir(
      Browser navegador, int ancho, boolean largos, String letra, List<String> fallas) {
    Page pagina = abrir(navegador, ancho);
    pagina.evaluate(ALARGAR, List.of(ROTULOS, largos, letra == null ? "" : letra));
    Object cruces = pagina.evaluate(CRUCES, ROTULOS);
    if (cruces instanceof List<?> lista) {
      String caso =
          ancho + " px" + (largos ? ", textos largos" : "") + (letra == null ? "" : ", " + letra);
      lista.forEach(falla -> fallas.add(caso + ": " + falla));
    }
    pagina.close();
  }

  private Page abrir(Browser navegador, int ancho) {
    Page pagina = navegador.newPage(new Browser.NewPageOptions().setViewportSize(ancho, 900));
    pagina.navigate("http://localhost:" + puerto + "/publico-portada");
    pagina.evaluate("() => document.fonts.ready");
    return pagina;
  }
}
