package grupo5.cliente.visual;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import java.nio.file.Path;
import java.util.UUID;
import java.util.function.Predicate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Recorrido de ingreso contra un Keycloak real (SPEC-06 B1-05, SPEC-09 F6). Corre contra un stack
 * ya levantado ({@code docker compose up}, con el cliente en {@code http://localhost:8090}): no
 * levanta nada por su cuenta, así sirve igual en local, en el preprod y en la demo.
 *
 * <p>Se activa con {@code INGRESO_REAL_URL} (la base del sitio); sin esa variable no corre, porque
 * necesita el stack. El admin sale de {@code INGRESO_REAL_ADMIN_EMAIL} e {@code
 * INGRESO_REAL_ADMIN_PASSWORD}, que son {@code AUTH_ADMIN_EMAIL} y {@code AUTH_ADMIN_PASSWORD} del
 * {@code .env}. Perfil {@code visual}, y por clase para no correr también la verificación de
 * pantallas: {@code mvn test -pl cliente-liviano -Pvisual -Dtest=IngresoConKeycloakRealTest}. Deja
 * los videos y, si algo falla, una captura en {@code target/ingreso-real/}. Cada corrida crea una
 * cuenta de donante nueva.
 *
 * <p>Si un paso falla, el mensaje dice dónde se quedó el navegador (URL, título y texto visible).
 */
@EnabledIfEnvironmentVariable(named = "INGRESO_REAL_URL", matches = ".+")
class IngresoConKeycloakRealTest {

  private static final Path VIDEOS = Path.of("target/ingreso-real");
  private static final String SUBMIT = "form input[type=submit], form button[type=submit]";
  private static final String CERRAR_SESION = "form[action$='/logout'] [type=submit]";
  private static final String MENU_DE_LA_CUENTA = "details.navegacion-cuenta > summary";
  private static final String TARJETA_PERSONA_DONANTE = "a.registro-tipo[href*='tipo=persona']";
  private static final double ESPERA_MS = 30_000;
  private static final int LARGO_DEL_TEXTO = 400;

  private static final String PISTA_DEL_ADMIN =
      "Sigue en el login de Keycloak: casi seguro INGRESO_REAL_ADMIN_EMAIL o"
          + " INGRESO_REAL_ADMIN_PASSWORD no coinciden con la cuenta admin del realm. El realm toma"
          + " AUTH_ADMIN_EMAIL y AUTH_ADMIN_PASSWORD del .env solo al importarse con la base"
          + " vacía: si el .env cambió después, hace falta docker compose down -v.";
  private static final String PISTA_DEL_DONANTE =
      "Sigue en Keycloak: si rechazó el formulario de registro, el texto de arriba dice por qué"
          + " (política de claves o un email ya usado).";

  /** La base del sitio. Se lee al correr y no al cargar la clase: sin la variable no hay nada. */
  private static String sitio() {
    String url = System.getenv("INGRESO_REAL_URL");
    int fin = url.length();
    while (fin > 0 && url.charAt(fin - 1) == '/') {
      fin--;
    }
    return url.substring(0, fin);
  }

  private static Playwright playwright;
  private static Browser navegador;

  @BeforeAll
  static void abrirElNavegador() {
    playwright = Playwright.create();
    navegador = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
  }

  @AfterAll
  static void cerrarElNavegador() {
    navegador.close();
    playwright.close();
  }

  private BrowserContext contexto() {
    BrowserContext contexto =
        navegador.newContext(new Browser.NewContextOptions().setRecordVideoDir(VIDEOS));
    contexto.setDefaultTimeout(ESPERA_MS);
    return contexto;
  }

  private static boolean enKeycloak(String url) {
    return url.contains("/realms/donatrack/");
  }

  private static boolean enElSitio(String url) {
    return url.startsWith(sitio()) && !enKeycloak(url);
  }

  /**
   * Espera a que el navegador llegue a una URL sin esperar a que la página termine de cargar: lo
   * que se quiere saber es a dónde fue, no si una pantalla con datos tarda en pintarse.
   */
  private static void esperarLaUrl(Page pagina, Predicate<String> condicion) {
    pagina.waitForURL(condicion, new Page.WaitForURLOptions().setWaitUntil(WaitUntilState.COMMIT));
  }

  /**
   * Dónde se quedó el navegador, para que un fallo se explique solo: URL, título, texto y captura.
   */
  private static String diagnostico(Page pagina, String nombre, String pistaSiSigueEnKeycloak) {
    StringBuilder datos = new StringBuilder("\n── Dónde se quedó el navegador ──\nURL: ");
    datos.append(pagina.url());
    try {
      datos.append("\nTítulo: ").append(pagina.title());
    } catch (RuntimeException e) {
      datos.append("\nTítulo: (no disponible)");
    }
    try {
      String texto =
          pagina
              .locator("body")
              .innerText(new Locator.InnerTextOptions().setTimeout(3_000))
              .replace('\n', ' ');
      datos.append("\nTexto: ").append(texto, 0, Math.min(LARGO_DEL_TEXTO, texto.length()));
    } catch (RuntimeException e) {
      datos.append("\nTexto: (no disponible)");
    }
    if (enKeycloak(pagina.url())) {
      datos.append("\nPista: ").append(pistaSiSigueEnKeycloak);
    }
    try {
      pagina.screenshot(new Page.ScreenshotOptions().setPath(VIDEOS.resolve(nombre + ".png")));
      datos.append("\nCaptura: ").append(VIDEOS.resolve(nombre + ".png"));
    } catch (RuntimeException e) {
      datos.append("\nCaptura: (no disponible)");
    }
    return datos.toString();
  }

  /** Cierra sesión y comprueba que volver a entrar pide la clave (cayó la sesión de Keycloak). */
  private static void cerrarSesionYVerificarQuePideLaClave(Page pagina, String pantallaProtegida) {
    Locator cerrarSesion = pagina.locator(CERRAR_SESION).first();
    if (!cerrarSesion.isVisible()) {
      // En las pantallas con la cuenta, el botón vive en el menú de la foto (cuenta-menu.html).
      pagina.locator(MENU_DE_LA_CUENTA).first().click();
    }
    cerrarSesion.click();
    pagina.waitForURL(url -> url.equals(sitio() + "/"));

    pagina.navigate(sitio() + pantallaProtegida);
    pagina.waitForURL(IngresoConKeycloakRealTest::enKeycloak);
    assertTrue(
        pagina.locator("input[name=password]").count() > 0,
        "Después de cerrar sesión, volver a entrar tiene que pedir la clave");
  }

  @Test
  void unDonanteSeRegistraCompletaQueFaltaSuPerfilYCierraSesion() {
    String email = "donante.e2e." + System.currentTimeMillis() + "@example.com";
    String clave = "Clave-e2e-" + UUID.randomUUID();

    try (BrowserContext contexto = contexto()) {
      Page pagina = contexto.newPage();
      try {
        // "Crear mi cuenta" manda a Keycloak directo al formulario de registro (prompt=create).
        pagina.navigate(sitio() + "/publico-registro-donante");
        pagina.waitForURL(IngresoConKeycloakRealTest::enKeycloak);
        pagina.locator("input[name=email]").fill(email);
        pagina.locator("input[name=password]").fill(clave);
        pagina.locator("input[name=password-confirm]").fill(clave);
        pagina.locator(SUBMIT).first().click();

        // Una cuenta sin perfil vuelve al sitio y cae en la pantalla para elegir el tipo de cuenta
        // (EstadoDeCuenta.SIN_PERFIL); recién de ahí se llega al formulario de cada tipo.
        pagina.waitForURL(url -> url.equals(sitio() + "/publico-registro"));
        pagina.locator(TARJETA_PERSONA_DONANTE).click();
        pagina.waitForURL(url -> url.startsWith(sitio() + "/publico-registro-donante"));
        pagina.locator("form.registro-formulario").first().waitFor();

        cerrarSesionYVerificarQuePideLaClave(pagina, "/donante-inicio");
      } catch (AssertionError | RuntimeException e) {
        throw new AssertionError(
            e.getMessage() + diagnostico(pagina, "donante", PISTA_DEL_DONANTE), e);
      }
    }
  }

  @Test
  void elAdminIngresaVeSuInicioYCierraSesion() {
    String email = System.getenv("INGRESO_REAL_ADMIN_EMAIL");
    String clave = System.getenv("INGRESO_REAL_ADMIN_PASSWORD");
    assertNotNull(email, "Falta INGRESO_REAL_ADMIN_EMAIL (AUTH_ADMIN_EMAIL del .env)");
    assertNotNull(clave, "Falta INGRESO_REAL_ADMIN_PASSWORD (AUTH_ADMIN_PASSWORD del .env)");

    try (BrowserContext contexto = contexto()) {
      Page pagina = contexto.newPage();
      try {
        pagina.navigate(sitio() + "/publico-ingresar");
        pagina.waitForURL(IngresoConKeycloakRealTest::enKeycloak);
        pagina.locator("input[name=username]").fill(email);
        pagina.locator("input[name=password]").fill(clave);
        pagina.locator(SUBMIT).first().click();

        // Si la clave no sirve, Keycloak vuelve a mostrar el login y esto vence: el diagnóstico lo
        // dice.
        esperarLaUrl(pagina, url -> enElSitio(url) && url.contains("/admin-"));

        cerrarSesionYVerificarQuePideLaClave(pagina, "/admin-inicio");
      } catch (AssertionError | RuntimeException e) {
        throw new AssertionError(e.getMessage() + diagnostico(pagina, "admin", PISTA_DEL_ADMIN), e);
      }
    }
  }
}
