package grupo5.cliente.visual;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * R11-16: el menú de la cuenta y las pastillas de filtros se cierran al tocar afuera, con Esc y al
 * perder el foco; sin JavaScript siguen abriendo y cerrando como un {@code <details>}. Perfil Maven
 * {@code visual} (necesita Playwright): {@code mvn test -pl cliente-liviano -Pvisual
 * -Dtest=MenusQueSeCierranTest}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("maqueta")
class MenusQueSeCierranTest {

  private static final String CUENTA = "details.navegacion-cuenta";
  private static final String FILTRO = "details.filtros-pildora";

  @LocalServerPort private int puerto;

  @Test
  void elMenuDeLaCuentaSeCierraTocandoAfuera() {
    conPagina(
        "/donante-inicio",
        true,
        pagina -> {
          abrir(pagina, CUENTA);
          pagina.locator("#contenido h1").first().click();
          assertThat(estaAbierto(pagina, CUENTA)).isFalse();
        });
  }

  @Test
  void elMenuDeLaCuentaSeCierraConEscYDevuelveElFoco() {
    conPagina(
        "/donante-inicio",
        true,
        pagina -> {
          abrir(pagina, CUENTA);
          pagina.locator(CUENTA + " a").first().focus();
          pagina.keyboard().press("Escape");
          assertThat(estaAbierto(pagina, CUENTA)).isFalse();
          assertThat(
                  pagina.evaluate(
                      "() => document.activeElement.matches('" + CUENTA + " > summary')"))
              .isEqualTo(true);
        });
  }

  @Test
  void elMenuDeLaCuentaSeCierraAlPerderElFoco() {
    conPagina(
        "/donante-inicio",
        true,
        pagina -> {
          abrir(pagina, CUENTA);
          pagina.locator(CUENTA + " a").first().focus();
          pagina.locator("#contenido").focus();
          // El menú se cierra un instante después de que el foco llega a destino.
          pagina.waitForFunction("() => !document.querySelector('details.navegacion-cuenta').open");
        });
  }

  @Test
  void tocarAdentroDelMenuNoLoCierra() {
    conPagina(
        "/donante-inicio",
        true,
        pagina -> {
          abrir(pagina, CUENTA);
          pagina.locator(CUENTA + " p").first().click();
          assertThat(estaAbierto(pagina, CUENTA)).isTrue();
        });
  }

  @Test
  void lasPastillasDeFiltroSeCierranTocandoAfueraYConEsc() {
    conPagina(
        "/admin-donantes",
        true,
        pagina -> {
          abrir(pagina, FILTRO);
          pagina.locator(FILTRO + "[open] legend").first().click();
          assertThat(estaAbierto(pagina, FILTRO)).as("tocar adentro").isTrue();
          pagina.locator("#contenido h1").first().click();
          assertThat(estaAbierto(pagina, FILTRO)).as("tocar afuera").isFalse();

          abrir(pagina, FILTRO);
          pagina.keyboard().press("Escape");
          assertThat(estaAbierto(pagina, FILTRO)).as("Esc").isFalse();
        });
  }

  @Test
  void abrirUnaPastillaCierraLaQueEstabaAbierta() {
    conPagina(
        "/admin-donantes",
        true,
        pagina -> {
          pagina.locator(FILTRO + " > summary").nth(0).click();
          pagina.locator(FILTRO + " > summary").nth(1).click();
          assertThat(pagina.locator(FILTRO + "[open]").count()).isEqualTo(1);
          assertThat(pagina.locator(FILTRO).nth(1).getAttribute("open")).isNotNull();
        });
  }

  @Test
  void sinJavaScriptSigueAbriendoYCerrandoComoAntes() {
    conPagina(
        "/admin-donantes",
        false,
        pagina -> {
          pagina.locator(FILTRO + " > summary").first().click();
          assertThat(estaAbierto(pagina, FILTRO)).isTrue();
          pagina.locator(FILTRO + " > summary").first().click();
          assertThat(estaAbierto(pagina, FILTRO)).isFalse();
          pagina.locator(CUENTA + " > summary").click();
          assertThat(estaAbierto(pagina, CUENTA)).isTrue();
          pagina.locator(CUENTA + " > summary").click();
          assertThat(estaAbierto(pagina, CUENTA)).isFalse();
        });
  }

  private void conPagina(
      String ruta, boolean conJavaScript, java.util.function.Consumer<Page> prueba) {
    try (Playwright playwright = Playwright.create();
        Browser navegador = playwright.chromium().launch();
        BrowserContext contexto =
            navegador.newContext(
                new Browser.NewContextOptions().setJavaScriptEnabled(conJavaScript))) {
      Page pagina = contexto.newPage();
      pagina.navigate("http://localhost:" + puerto + ruta);
      pagina.waitForLoadState();
      prueba.accept(pagina);
    }
  }

  private static void abrir(Page pagina, String selector) {
    pagina.locator(selector + " > summary").first().click();
    assertThat(estaAbierto(pagina, selector)).as("se abrió " + selector).isTrue();
  }

  private static boolean estaAbierto(Page pagina, String selector) {
    return pagina.locator(selector + "[open]").count() > 0;
  }
}
