package grupo5.cliente.visual;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Path;
import java.util.UUID;
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
 * los videos en {@code target/ingreso-real/}. Cada corrida crea una cuenta de donante nueva.
 */
@EnabledIfEnvironmentVariable(named = "INGRESO_REAL_URL", matches = ".+")
class IngresoConKeycloakRealTest {

  private static final Path VIDEOS = Path.of("target/ingreso-real");
  private static final String SUBMIT = "form input[type=submit], form button[type=submit]";
  private static final String CERRAR_SESION = "form[action$='/logout'] [type=submit]";
  private static final double ESPERA_MS = 30_000;

  /** La base del sitio. Se lee al correr y no al cargar la clase: sin la variable no hay nada. */
  private static String sitio() {
    return System.getenv("INGRESO_REAL_URL").replaceAll("/+$", "");
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

  /** Cierra sesión y comprueba que volver a entrar pide la clave (cayó la sesión de Keycloak). */
  private static void cerrarSesionYVerificarQuePideLaClave(Page pagina, String pantallaProtegida) {
    pagina.locator(CERRAR_SESION).first().click();
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

      // "Crear mi cuenta" manda a Keycloak directo al formulario de registro (prompt=create).
      pagina.navigate(sitio() + "/publico-registro-donante");
      pagina.waitForURL(IngresoConKeycloakRealTest::enKeycloak);
      pagina.locator("input[name=email]").fill(email);
      pagina.locator("input[name=password]").fill(clave);
      pagina.locator("input[name=password-confirm]").fill(clave);
      pagina.locator(SUBMIT).first().click();

      // Vuelve al sitio con la cuenta creada pero sin perfil: lo pide, y desde ahí se puede salir.
      pagina.waitForURL(IngresoConKeycloakRealTest::enElSitio);
      assertTrue(
          pagina.locator("form.registro-formulario").count() > 0,
          "Una cuenta sin perfil tiene que ver el formulario para completarlo: " + pagina.url());

      cerrarSesionYVerificarQuePideLaClave(pagina, "/donante-inicio");
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

      pagina.navigate(sitio() + "/publico-ingresar");
      pagina.waitForURL(IngresoConKeycloakRealTest::enKeycloak);
      pagina.locator("input[name=username]").fill(email);
      pagina.locator("input[name=password]").fill(clave);
      pagina.locator(SUBMIT).first().click();

      pagina.waitForURL(IngresoConKeycloakRealTest::enElSitio);
      assertTrue(
          pagina.url().contains("/admin-"),
          "El admin tiene que llegar a su área, no a otra: " + pagina.url());

      cerrarSesionYVerificarQuePideLaClave(pagina, "/admin-inicio");
    }
  }
}
