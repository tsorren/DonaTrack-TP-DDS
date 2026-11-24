package grupo5.cliente.maqueta;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import grupo5.cliente.web.TraceIdFiltro;
import jakarta.servlet.RequestDispatcher;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.autoconfigure.error.DefaultErrorViewResolver;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * Con el contexto entero del perfil maqueta: todos los controllers y los adaptadores de maqueta de
 * cada puerto, también los que suma la conexión de pantallas (SPEC-08), sin listarlos acá. La
 * pantalla de prueba está en src/test/resources/templates/.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(SitioControllerTest.PaginasDeError.class)
@ActiveProfiles("maqueta")
class SitioControllerTest {

  /**
   * Con MockMvc no hay DispatcherServlet de verdad que despache el error, y el resolvedor que elige
   * templates/error/&lt;código&gt; se registra acá, igual que en la aplicación.
   */
  @TestConfiguration
  static class PaginasDeError {

    @Bean
    DefaultErrorViewResolver resolvedorDePaginasDeError(ApplicationContext contexto) {
      return new DefaultErrorViewResolver(contexto, new WebProperties.Resources());
    }
  }

  @Autowired private MockMvc mvc;

  @Test
  void elInicioEsLaPortada() throws Exception {
    mvc.perform(get("/")).andExpect(status().isOk()).andExpect(view().name("publico-portada"));
  }

  @Test
  void unaSeccionSinPantallaMuestraQueNoEstaDibujada() throws Exception {
    mvc.perform(get("/entidad-seguimiento-tesoro"))
        .andExpect(status().isOk())
        .andExpect(view().name("sin-dibujar"))
        .andExpect(model().attribute("seccion", "Entregas"))
        .andExpect(content().string(containsString("todavía no está dibujada")))
        .andExpect(content().string(containsString("href=\"/entidad-inicio\"")));
  }

  @Test
  void loQueNoEstaNiEnElMenuNiDibujadoNoExiste() throws Exception {
    mvc.perform(get("/donante-tesoro")).andExpect(status().isNotFound());
    // El firewall de Spring Security rechaza la barra codificada antes de llegar al controller.
    mvc.perform(get("/..%2Ftemplates%2Fpie")).andExpect(status().isBadRequest());
    mvc.perform(get("/sin-dibujar")).andExpect(status().isNotFound());
    mvc.perform(get("/datos-donante")).andExpect(status().isNotFound());
    mvc.perform(get("/perfil")).andExpect(status().isNotFound());
  }

  // R11-10: cada administrador tiene su Mi perfil y la lista de Administradores, en el menú de la
  // cuenta (antes la administración no tenía Mi perfil).
  @Test
  void laAdministracionTieneMiPerfilYAdministradores() throws Exception {
    mvc.perform(get("/admin-inicio"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("href=\"/admin-perfil\"")))
        .andExpect(content().string(containsString("href=\"/admin-administradores\"")));
    mvc.perform(get("/admin-perfil"))
        .andExpect(status().isOk())
        .andExpect(view().name("admin-perfil"))
        .andExpect(model().attribute("seccion", "Mi perfil"));
    mvc.perform(get("/admin-administradores"))
        .andExpect(status().isOk())
        .andExpect(view().name("admin-administradores"))
        .andExpect(model().attribute("seccion", "Administradores"));
  }

  @Test
  void lasPantallasDeAdministracionMuestranSusEstadosEnLaMaqueta() throws Exception {
    mvc.perform(get("/admin-perfil").param("escenario", "vacio"))
        .andExpect(content().string(containsString("Todavía no cargaste tu nombre.")));
    mvc.perform(get("/admin-perfil").param("escenario", "error"))
        .andExpect(content().string(containsString("No pudimos traer tus datos ahora.")));
    mvc.perform(get("/admin-administradores").param("escenario", "error"))
        .andExpect(
            content()
                .string(containsString("No pudimos traer las cuentas de administración ahora.")));
    mvc.perform(get("/admin-administradores-nuevo")).andExpect(status().isOk());
    mvc.perform(get("/admin-administradores-alta"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("K7pQ-m4Rt-x9Wa-2Lse")));
  }

  @Test
  void unFormularioVuelveASuDestinoSinLoEscrito() throws Exception {
    mvc.perform(post("/donante-donaciones-prueba").param("contrasena", "x"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/donante-donaciones-prueba"));
    mvc.perform(post("/donante-tesoro")).andExpect(status().isNotFound());
  }

  @Test
  void lasHojasYLasImagenesSiguenSiendoEstaticas() throws Exception {
    mvc.perform(get("/css/tokens.css")).andExpect(status().isOk());
    mvc.perform(get("/img/logo.svg")).andExpect(status().isOk());
  }

  /**
   * B0-05 (D31): los tres layouts cargan Alpine CSP desde el WebJar con defer. La plantilla no
   * escribe la versión (vive en el pom.xml); Spring la agrega a la dirección.
   */
  @Test
  void losLayoutsCarganAlpineDesdeElWebJarDespuesDeSusComponentes() throws Exception {
    String alpine = "/webjars/alpinejs__csp/";
    for (String pantalla : List.of("/", "/publico-ingresar", "/donante-inicio")) {
      String html =
          mvc.perform(get(pantalla))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();
      assertTrue(
          Pattern.compile(
                  "<script[^>]*src=\"" + alpine + "[\\d.]+/dist/cdn\\.min\\.js\"[^>]*\\bdefer\\b")
              .matcher(html)
              .find(),
          pantalla + ": falta Alpine con defer");
      assertTrue(
          html.indexOf("/js/interfaz.js") > 0
              && html.indexOf("/js/interfaz.js") < html.indexOf(alpine),
          pantalla + ": interfaz.js va antes que Alpine");
    }
    mvc.perform(get(alpine + "dist/cdn.min.js")).andExpect(status().isOk());
    mvc.perform(get("/js/interfaz.js")).andExpect(status().isOk());
  }

  @Test
  void unaPantallaDeNivelTresMarcaSuSeccionEnElMenuDeSuRol() throws Exception {
    mvc.perform(get("/donante-donaciones-prueba"))
        .andExpect(status().isOk())
        .andExpect(view().name("donante-donaciones-prueba"))
        .andExpect(model().attribute("seccion", "Donaciones"))
        .andExpect(
            content().string(containsString("href=\"/donante-donaciones\" aria-current=\"page\"")));
  }

  @Test
  void loQueNoEstaDibujadoMuestraElAviso() throws Exception {
    mvc.perform(get("/donante-entidades-detalle?entidad=comedor"))
        .andExpect(status().isOk())
        .andExpect(view().name("sin-dibujar"))
        .andExpect(model().attribute("seccion", "Organizaciones"))
        .andExpect(content().string(containsString("Empresa donante")))
        .andExpect(content().string(not(containsString("entidad=comedor\""))));
    mvc.perform(get("/donante-incentivos")).andExpect(status().isNotFound());
    mvc.perform(get("/donante-tesoro")).andExpect(status().isNotFound());
    mvc.perform(get("/sin-dibujar")).andExpect(status().isNotFound());
    mvc.perform(get("/publico-perfil")).andExpect(status().isNotFound());
    mvc.perform(post("/donante-tesoro")).andExpect(status().isNotFound());
  }

  /**
   * Seguimiento es el arquetipo A07 (DA-03a): una sola donación, la primera en camino si no se pide
   * ninguna, con el mapa y su recorrido, y las migas que vuelven a su detalle. Sin hora estimada
   * (HG-02). La persona sigue a la empresa (DA-13).
   */
  @Test
  void elSeguimientoMuestraCadaEstado() throws Exception {
    mvc.perform(get("/donante-seguimiento"))
        .andExpect(status().isOk())
        .andExpect(view().name("donante-seguimiento"))
        // Seguimiento ya no es una sección del menú: se abre desde una donación (DA-03a).
        .andExpect(model().attribute("seccion", "Donaciones"))
        .andExpect(
            content().string(containsString("href=\"/donante-donaciones\" aria-current=\"page\"")))
        .andExpect(content().string(containsString("<h1>Lentejas secas, 15 kg</h1>")))
        .andExpect(
            content()
                .string(containsString("href=\"/donante-donaciones-detalle?donacion=lentejas\"")))
        .andExpect(content().string(containsString("data-lat=\"-34.6392\"")))
        .andExpect(content().string(containsString("aria-current=\"step\"")))
        .andExpect(content().string(containsString("aria-labelledby=\"recorrido-titulo\"")))
        .andExpect(content().string(not(containsString("Hora estimada"))));
    // La que no pudo llegar: el mismo recorrido que su detalle, sin fecha de nueva salida (HG-01),
    // y sin camión en el mapa, porque no está viajando (A08).
    mvc.perform(get("/donante-seguimiento?donacion=utiles"))
        .andExpect(content().string(containsString("<h1>Útiles escolares, 2 cajas</h1>")))
        .andExpect(content().string(containsString("data-paso=\"problema\"")))
        .andExpect(content().string(not(containsString("17 de octubre"))))
        .andExpect(content().string(not(containsString("data-mapa-puntos"))))
        .andExpect(content().string(not(containsString("Posición aproximada"))))
        .andExpect(content().string(containsString("Todavía sin posición del camión")))
        .andExpect(
            content().string(containsString("El camión no está viajando con esta donación.")));
    // La asignada todavía no salió: no hay camión que seguir (A06).
    mvc.perform(get("/donante-seguimiento?donacion=leche"))
        .andExpect(content().string(containsString("<h1>Leche en polvo")))
        .andExpect(content().string(not(containsString("data-mapa-puntos"))))
        .andExpect(content().string(containsString("Todavía sin posición del camión")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "Cuando el camión salga del depósito, vas a ver acá por dónde va.")));
    mvc.perform(get("/donante-seguimiento?cuenta=persona&donacion=leche"))
        .andExpect(view().name("persona/donante-seguimiento"))
        .andExpect(content().string(not(containsString("data-mapa-puntos"))))
        .andExpect(content().string(containsString("Todavía sin posición del camión")));
    mvc.perform(get("/donante-seguimiento?donacion=fideos"))
        .andExpect(content().string(containsString("No encontramos esa donación en camino.")));
    mvc.perform(get("/donante-seguimiento?cuenta=persona"))
        .andExpect(view().name("persona/donante-seguimiento"))
        .andExpect(content().string(containsString("<h1>Ropa de abrigo, 12 prendas</h1>")))
        .andExpect(content().string(containsString("Todavía espera destino")));
    mvc.perform(get("/donante-seguimiento?cuenta=empresa&escenario=vacio"))
        .andExpect(
            content().string(containsString("Ningún camión está en camino con tus donaciones")));
    mvc.perform(get("/donante-seguimiento?escenario=error"))
        .andExpect(content().string(containsString("role=\"alert\"")))
        .andExpect(content().string(containsString("Volver a intentar")));
    mvc.perform(get("/donante-seguimiento?escenario=lento"))
        .andExpect(content().string(containsString("aria-busy=\"true\"")));
  }

  /**
   * La persona recorre su detalle en camino, el enlace a su seguimiento y la vuelta (DA-03a y
   * DA-13): las dos pantallas salen de sus datos (persona/datos-persona), no de los de la empresa.
   */
  @Test
  void laPersonaVaDelDetalleAlSeguimientoYVuelveConSusDatos() throws Exception {
    MockHttpSession sesion = new MockHttpSession();
    mvc.perform(get("/donante-donaciones-detalle?cuenta=persona").session(sesion))
        .andExpect(view().name("persona/donante-donaciones-detalle"))
        .andExpect(content().string(containsString("<h1>Ropa de abrigo, 12 prendas</h1>")))
        .andExpect(content().string(containsString("Donación 0006. Para Merendero Ejemplo Sur.")))
        .andExpect(content().string(containsString("figurás como «Martina Ruiz».")))
        .andExpect(content().string(not(containsString("Distribuidora Ejemplo SA"))))
        .andExpect(content().string(containsString("href=\"/donante-seguimiento?donacion=ropa\"")));
    mvc.perform(get("/donante-seguimiento?donacion=ropa").session(sesion))
        .andExpect(view().name("persona/donante-seguimiento"))
        .andExpect(content().string(containsString("<h1>Ropa de abrigo, 12 prendas</h1>")))
        .andExpect(content().string(not(containsString("No encontramos esa donación en camino."))))
        .andExpect(content().string(containsString("Donación 0006. Para Merendero Ejemplo Sur.")))
        .andExpect(
            content().string(containsString("En camino desde las 11:10. Posición aproximada")))
        .andExpect(
            content().string(containsString("href=\"/donante-donaciones-detalle?donacion=ropa\"")));
    mvc.perform(get("/donante-donaciones-detalle?donacion=ropa").session(sesion))
        .andExpect(view().name("persona/donante-donaciones-detalle"))
        .andExpect(content().string(containsString("<h1>Ropa de abrigo, 12 prendas</h1>")))
        .andExpect(content().string(containsString("data-estado=\"en-camino\"")))
        .andExpect(content().string(not(containsString("confirmó la entrega el 9 de octubre"))));
    // Las lentejas de la persona llegaron en junio: su detalle es el de una entregada, sin enlace
    // al seguimiento; una clave que la persona no tiene, "No encontramos esa donación".
    mvc.perform(get("/donante-donaciones-detalle?donacion=lentejas").session(sesion))
        .andExpect(content().string(containsString("<h1>Lentejas secas, 4 kg</h1>")))
        .andExpect(content().string(containsString("class=\"etiqueta-entrega\"")))
        .andExpect(content().string(not(containsString("/donante-seguimiento?donacion="))))
        // El sello y la raíz dicen lo mismo que la lista: «Entrega confirmada», no «Entregada».
        .andExpect(
            content()
                .string(
                    matchesPattern(
                        "(?s).*<span class=\"estado\"\\s+data-estado=\"entregada\">"
                            + "(?:(?!</span>\\s*</div>).)*?Entrega confirmada</span>.*")))
        .andExpect(content().string(not(containsString("Entregada</"))))
        .andExpect(content().string(not(containsString("En depósito"))));
    // La que espera destino: Registrada, como en la lista, no Pendiente.
    mvc.perform(get("/donante-donaciones-detalle?donacion=fideos").session(sesion))
        .andExpect(content().string(containsString("data-estado=\"registrada\"")))
        .andExpect(content().string(not(containsString("data-estado=\"pendiente\""))))
        .andExpect(
            content()
                .string(
                    matchesPattern(
                        "(?s).*<span class=\"estado\"\\s+data-estado=\"registrada\">"
                            + "(?:(?!</span>\\s*</div>).)*?Registrada</span>.*")));
    // La asignada en su seguimiento: Asignada, como en la lista y el detalle, no Pendiente.
    mvc.perform(get("/donante-seguimiento?donacion=leche").session(sesion))
        .andExpect(content().string(containsString("<h1>Leche en polvo, 4 unidades</h1>")))
        .andExpect(content().string(containsString("data-estado=\"asignada\"")))
        .andExpect(content().string(not(containsString("data-estado=\"pendiente\""))))
        .andExpect(content().string(containsString("Donación 0007. Para ")));
    mvc.perform(get("/donante-donaciones-detalle?donacion=pan").session(sesion))
        .andExpect(content().string(containsString("No encontramos esa donación.")));
  }

  /**
   * DA-03a, DA-03b y DA-05: el menú de la empresa es Inicio, Donaciones, Organizaciones y Cómo
   * donar (Para donantes); el de la persona de ejemplo (conMisiones de CuentaDeEjemplo) suma
   * Misiones antes de Cómo donar. Ninguno tiene Seguimiento, que sigue respondiendo.
   */
  @Test
  void losMenusDeQuienDonaSonLosDeLaGuia() throws Exception {
    String empresa =
        mvc.perform(get("/donante-inicio?cuenta=empresa"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertEquals(
        List.of(
            "/donante-inicio Inicio",
            "/donante-donaciones Donaciones",
            "/donante-entidades Organizaciones",
            "/publico-para-donantes Cómo donar"),
        entradasDelMenu(empresa));
    String persona =
        mvc.perform(get("/donante-inicio?cuenta=persona"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertEquals(
        List.of(
            "/donante-inicio Inicio",
            "/donante-donaciones Donaciones",
            "/donante-entidades Organizaciones",
            "/donante-misiones Misiones",
            "/publico-para-donantes Cómo donar"),
        entradasDelMenu(persona));
    mvc.perform(get("/donante-seguimiento?cuenta=empresa")).andExpect(status().isOk());
  }

  private static final Pattern MENU_DE_LA_PAGINA =
      Pattern.compile("<nav[^>]*id=\"menu\"[^>]*>(.*?)</nav>", Pattern.DOTALL);
  private static final Pattern ENTRADA_DEL_MENU =
      Pattern.compile("<a href=\"([^\"]+)\"[^>]*>.*?<span>([^<]+)</span></a>", Pattern.DOTALL);

  /** Las entradas del menú del layout privado ({@code nav#menu}), como "dirección texto". */
  private static List<String> entradasDelMenu(String html) {
    Matcher nav = MENU_DE_LA_PAGINA.matcher(html);
    assertTrue(nav.find(), "la página tiene el menú");
    Matcher entrada = ENTRADA_DEL_MENU.matcher(nav.group(1));
    List<String> entradas = new ArrayList<>();
    while (entrada.find()) {
      entradas.add(entrada.group(1) + " " + entrada.group(2));
    }
    return entradas;
  }

  /**
   * Donaciones de la persona sigue a la de la empresa (DA-13; arquetipo A05): la misma tabla, con
   * una raíz y un sello por fila, y las vistas por estado con su cuenta sobre sus datos de ejemplo.
   */
  @Test
  void lasDonacionesDeLaPersonaSonLaMismaTablaConSusDatos() throws Exception {
    mvc.perform(get("/donante-donaciones?cuenta=persona"))
        .andExpect(status().isOk())
        .andExpect(view().name("persona/donante-donaciones"))
        .andExpect(content().string(containsString("<h1>Donaciones</h1>")))
        .andExpect(content().string(containsString("<table class=\"tabla\">")))
        .andExpect(content().string(containsString("Donación 0008")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "aria-label=\"El recorrido de esta donación: va por el paso 4 de 5\"")))
        .andExpect(content().string(containsString(">Todas <b>8</b></a>")))
        .andExpect(content().string(containsString(">Confirmadas <b>5</b></a>")))
        .andExpect(content().string(containsString(">Con problema <b>0</b></a>")))
        // El sello dice lo mismo que la raíz y el detalle (A05, A06): la pendiente sin destino,
        // registrada; con destino, asignada; la entregada, «Entrega confirmada».
        .andExpect(
            content()
                .string(
                    matchesPattern(
                        "(?s).*<span class=\"estado\"\\s+data-estado=\"registrada\">"
                            + "(?:(?!</td>).)*?</span>"
                            + "\\s*Registrada</span>.*")))
        .andExpect(
            content()
                .string(
                    matchesPattern(
                        "(?s).*<span class=\"estado\" data-estado=\"asignada\">"
                            + "(?:(?!</td>).)*?</span>"
                            + "\\s*Asignada</span>.*")))
        .andExpect(
            content()
                .string(
                    matchesPattern(
                        "(?s).*<span class=\"estado\"\\s+data-estado=\"entregada\">"
                            + "(?:(?!</td>).)*?</span>"
                            + "\\s*Entrega confirmada</span>.*")))
        .andExpect(content().string(not(containsString("data-estado=\"pendiente\""))));
    mvc.perform(get("/donante-donaciones?cuenta=persona&estado=en-camino"))
        .andExpect(content().string(containsString("1 donación con esos filtros.")))
        .andExpect(content().string(containsString("Ropa de abrigo")))
        .andExpect(content().string(not(containsString("Fideos secos"))))
        .andExpect(
            content()
                .string(
                    containsString(
                        "href=\"/donante-donaciones?cuenta=persona&amp;estado=en-camino\""
                            + " aria-current=\"page\">En camino <b>1</b></a>")))
        .andExpect(content().string(containsString(">Todas <b>8</b></a>")));
  }

  @Test
  void elDonanteRecorreSuInicioSusDonacionesEntidadesYNotificaciones() throws Exception {
    mvc.perform(get("/donante-inicio"))
        .andExpect(status().isOk())
        .andExpect(view().name("donante-inicio"))
        .andExpect(content().string(containsString("Útiles escolares, 2 cajas: no pudo llegar")))
        .andExpect(content().string(containsString("Seguir esta donación")))
        .andExpect(content().string(containsString("aria-label=\"Notificaciones, 4 sin leer\"")));
    mvc.perform(get("/donante-inicio?escenario=lento"))
        .andExpect(content().string(containsString("aria-busy=\"true\"")));
    mvc.perform(get("/donante-inicio?escenario=sin-camino"))
        .andExpect(content().string(containsString("Nada en camino ahora")))
        .andExpect(content().string(not(containsString("Seguir esta donación"))));
    mvc.perform(get("/donante-donaciones?estado=pendiente&categoria=alimentos"))
        .andExpect(content().string(containsString("2 donaciones con esos filtros.")))
        // Arroz sin destino va registrada; Leche, con destino, asignada, como en su detalle.
        .andExpect(
            content()
                .string(
                    matchesPattern(
                        "(?s).*<span class=\"estado\"\\s+data-estado=\"registrada\">"
                            + "(?:(?!</td>).)*?</span>"
                            + "\\s*Registrada</span>.*")))
        .andExpect(
            content()
                .string(
                    matchesPattern(
                        "(?s).*<span class=\"estado\" data-estado=\"asignada\">"
                            + "(?:(?!</td>).)*?</span>"
                            + "\\s*Asignada</span>.*")));
    mvc.perform(get("/donante-donaciones?estado=otro"))
        .andExpect(content().string(containsString("7 donaciones, de la más reciente")));
    // Mis donaciones tiene su controller, pero ?cuenta= sigue cambiando la cuenta de ejemplo.
    mvc.perform(get("/donante-donaciones?cuenta=persona"))
        .andExpect(content().string(containsString("Martina Ruiz")));
    mvc.perform(get("/donante-donaciones-detalle?donacion=ropa"))
        .andExpect(model().attribute("seccion", "Donaciones"))
        .andExpect(content().string(containsString("confirmó la entrega el 9 de octubre")))
        .andExpect(content().string(containsString("class=\"etiqueta-entrega\"")));
    // Las tres variantes de la guía: asignada (A06), en camino (A07, con el enlace a su
    // seguimiento, DA-03a) y con problema (A08, sin fecha de nueva salida, HG-01).
    mvc.perform(get("/donante-donaciones-detalle?donacion=leche"))
        .andExpect(content().string(containsString("data-estado=\"asignada\"")))
        .andExpect(content().string(not(containsString("/donante-seguimiento?donacion="))));
    mvc.perform(get("/donante-donaciones-detalle?donacion=lentejas"))
        .andExpect(
            content().string(containsString("href=\"/donante-seguimiento?donacion=lentejas\"")));
    mvc.perform(get("/donante-donaciones-detalle?donacion=utiles"))
        .andExpect(content().string(containsString("<dd>Todavía sin fecha de nueva salida</dd>")))
        .andExpect(content().string(not(containsString("17 de octubre"))));
    mvc.perform(get("/donante-donaciones-detalle?donacion=pan"))
        .andExpect(content().string(containsString("No encontramos esa donación.")));
    mvc.perform(get("/donante-entidades?nombre=zzz"))
        .andExpect(content().string(containsString("Ninguna organización activa se llama así.")));
    // Sin tildes ni mayúsculas, como la búsqueda de Donaciones (R11-13).
    mvc.perform(get("/donante-entidades?nombre=CLÚB"))
        .andExpect(content().string(containsString("Club Ejemplo Norte")))
        .andExpect(
            content().string(containsString("1 organización activa tiene «CLÚB» en el nombre.")));
    // R11-18: la segunda página de la maqueta trae los nombres largos y una entidad sin pedidos.
    mvc.perform(get("/donante-entidades?pagina=2"))
        .andExpect(
            content()
                .string(
                    containsString("CooperativaDeTrabajoYAyudaMutuaDelBarrioEjemploNorteYAledaño")))
        .andExpect(content().string(containsString("Hoy no pide nada.")));
    mvc.perform(get("/donante-entidades?pagina=2&cuenta=persona"))
        .andExpect(content().string(containsString("y el Deporte Sur")))
        .andExpect(content().string(containsString("Página 2 de 8.")));
    mvc.perform(get("/donante-entidades?escenario=error"))
        .andExpect(content().string(containsString("role=\"alert\"")))
        .andExpect(content().string(containsString("Cómo llega tu donación")));
    mvc.perform(post("/donante-notificaciones-leidas"))
        .andExpect(redirectedUrl("/donante-notificaciones-leidas"));
    mvc.perform(get("/donante-notificaciones-leidas"))
        .andExpect(content().string(containsString("Listo: marcaste todas como leídas.")))
        .andExpect(content().string(containsString("aria-label=\"Notificaciones\"")));
  }

  @Test
  void elActaMuestraCadaEstadoYConfirmaConUnFormularioReal() throws Exception {
    mvc.perform(get("/entidad-seguimiento-confirmar"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("seccion", "Entregas"))
        .andExpect(
            content()
                .string(
                    containsString(
                        "<strong>Lentejas secas, 15 kg</strong>, de Distribuidora Ejemplo SA."
                            + " Llegó hoy a las 18:20.")))
        .andExpect(content().string(containsString("enctype=\"multipart/form-data\"")))
        .andExpect(
            content().string(containsString("action=\"/entidad-seguimiento-sellada-lentejas\"")))
        .andExpect(content().string(containsString("aria-label=\"Dónde estás\"")))
        .andExpect(content().string(containsString("href=\"/entidad-seguimiento\">Entregas</a>")))
        .andExpect(content().string(containsString("aria-current=\"page\">Confirmar recepción")))
        .andExpect(content().string(containsString("Recibe Comedor Barrio Ejemplo, el 15 de")))
        .andExpect(content().string(containsString("data-papel=\"tierra\"")))
        .andExpect(content().string(containsString("name=\"fotos\"")));
    mvc.perform(get("/entidad-seguimiento-confirmar?entrega=ropa"))
        .andExpect(content().string(containsString(", de Donante Sostenedor.")));
    mvc.perform(get("/entidad-seguimiento-confirmar?entrega=pan"))
        .andExpect(content().string(containsString("No encontramos esa entrega")));
    mvc.perform(get("/entidad-seguimiento-confirmar?escenario=confirmada"))
        .andExpect(content().string(containsString("Ya confirmaste esta entrega")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "La confirmó Comedor Barrio Ejemplo el 15 de octubre de 2026, 18:45.")))
        .andExpect(
            content()
                .string(containsString("href=\"/entidad-seguimiento-sellada?entrega=lentejas\"")))
        .andExpect(content().string(not(containsString("name=\"fotos\""))))
        .andExpect(content().string(not(containsString("Van 10 kg recibidos."))));
    mvc.perform(get("/entidad-seguimiento-confirmar?escenario=sin-conexion"))
        .andExpect(content().string(containsString("No sabemos si la confirmación llegó.")));
    mvc.perform(get("/entidad-seguimiento-confirmar-error"))
        .andExpect(content().string(containsString("Revisá 2 cosas para confirmar")))
        .andExpect(content().string(containsString("href=\"#fotos\">La foto lentejas-1.jpg pesa")))
        .andExpect(content().string(containsString("aria-invalid=\"true\"")));
  }

  @Test
  void confirmarLlevaALaRaizCompletaYALaQueSigue() throws Exception {
    mvc.perform(post("/entidad-seguimiento-sellada-lentejas"))
        .andExpect(redirectedUrl("/entidad-seguimiento-sellada-lentejas"));
    mvc.perform(get("/entidad-seguimiento-sellada-lentejas"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("huerta-raiz-trazo")))
        .andExpect(content().string(containsString("data-papel=\"protagonista\"")))
        .andExpect(content().string(containsString("aria-current=\"page\">Recepción confirmada")))
        .andExpect(content().string(containsString("<dd>0001-00004521</dd>")))
        .andExpect(
            content().string(containsString("Recibió: Comedor Barrio Ejemplo, el 15 de octubre.")))
        .andExpect(content().string(containsString("Foto de ejemplo")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "7 entregas confirmadas</strong> por tu organización en octubre.")))
        .andExpect(content().string(containsString("data-recien=\"si\"")))
        .andExpect(content().string(containsString(">12 de octubre, 10:00</span>")))
        .andExpect(content().string(containsString(">14 de octubre, 9:10</span>")))
        .andExpect(content().string(not(containsString("Marta"))))
        .andExpect(content().string(containsString("Te queda una en la puerta")))
        .andExpect(content().string(containsString("entidad-seguimiento-confirmar?entrega=ropa")));
    mvc.perform(get("/entidad-seguimiento-sellada?entrega=lentejas"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Recepción confirmada")))
        .andExpect(content().string(not(containsString("huerta-raiz-trazo"))));
    mvc.perform(get("/entidad-seguimiento-sellada-ropa"))
        .andExpect(content().string(containsString("No te quedan entregas para confirmar.")))
        .andExpect(content().string(not(containsString("Te queda una en la puerta"))))
        .andExpect(content().string(containsString("15 de octubre de 2026, 18:50")));
  }

  @Test
  void lasNecesidadesMuestranCadaEstadoYSusAcciones() throws Exception {
    mvc.perform(get("/entidad-necesidades"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("seccion", "Necesidades"))
        .andExpect(recibidos("10", "de 40 kg recibidos"))
        .andExpect(content().string(containsString("Llegó una parte")))
        .andExpect(content().string(containsString("aria-current=\"true\"")))
        .andExpect(content().string(containsString("popover")))
        .andExpect(content().string(containsString("action=\"/entidad-necesidades-publicada\"")))
        .andExpect(content().string(containsString("Cumplidas y terminadas (3)")));
    mvc.perform(get("/entidad-necesidades?escenario=vacio"))
        .andExpect(content().string(containsString("No tenés necesidades vigentes.")));
    mvc.perform(get("/entidad-necesidades?escenario=error"))
        .andExpect(content().string(containsString("No pudimos cargar tus necesidades.")))
        .andExpect(content().string(containsString("role=\"alert\"")));
    mvc.perform(get("/entidad-necesidades?escenario=lento"))
        .andExpect(content().string(containsString("aria-busy=\"true\"")));
    mvc.perform(get("/entidad-necesidades?necesidad=pan"))
        .andExpect(content().string(containsString("No encontramos esa necesidad.")));
    mvc.perform(get("/entidad-necesidades?escenario=conflicto"))
        .andExpect(
            content()
                .string(containsString("Otra persona de tu organización cambió esta necesidad")));
    mvc.perform(get("/entidad-necesidades?escenario=entidad-inactiva"))
        .andExpect(content().string(containsString("Tu organización no está activa")))
        .andExpect(content().string(not(containsString("Publicar necesidad"))));
    mvc.perform(get("/entidad-necesidades-publicar-error"))
        .andExpect(content().string(containsString("Revisá dos cosas antes de publicar")))
        .andExpect(content().string(containsString("aria-invalid=\"true\"")));
  }

  /**
   * R11-32 y R11-31 (ficha r11-p2-ventanas): las ventanas de la regla 4 llevan data-bloquea sobre
   * el popover de hoy (sin JavaScript siguen andando igual); Publicar otra necesidad es un botón
   * que abre la ventana en pasos, con los tres grupos y sin el formulario plegado.
   */
  @Test
  void lasVentanasDeLaRegla4BloqueanYPublicarVaEnPasos() throws Exception {
    mvc.perform(get("/entidad-necesidades"))
        .andExpect(
            content()
                .string(matchesPattern("(?s).*id=\"baja-lentejas\" popover[^>]*data-bloquea.*")))
        .andExpect(content().string(containsString("popovertarget=\"publicar-ventana\"")))
        .andExpect(
            content().string(matchesPattern("(?s).*id=\"publicar-ventana\"[^>]*data-bloquea.*")))
        .andExpect(content().string(containsString("data-forma=\"pasos\"")))
        .andExpect(content().string(containsString("Paso 2 de 3: </span>Cuándo")))
        .andExpect(content().string(containsString("Vas a publicar")))
        .andExpect(content().string(containsString("Elegí qué hace falta.")))
        .andExpect(content().string(containsString("No sabemos si se publicó.")))
        .andExpect(content().string(not(containsString("id=\"publicar\""))));
    // Sin vigentes o de vuelta con un error: el formulario en la página, sin ventana.
    mvc.perform(get("/entidad-necesidades?escenario=vacio"))
        .andExpect(content().string(containsString("Publicar una necesidad")))
        .andExpect(content().string(not(containsString("id=\"publicar-ventana\""))));
    mvc.perform(get("/entidad-necesidades-publicar-error"))
        .andExpect(content().string(not(containsString("id=\"publicar-ventana\""))))
        .andExpect(content().string(containsString("Revisá dos cosas antes de publicar")));
    mvc.perform(get("/admin-camiones"))
        .andExpect(
            content()
                .string(matchesPattern("(?s).*id=\"agregar-camion\" popover[^>]*data-bloquea.*")))
        .andExpect(content().string(containsString("No sabemos si se guardó.")));
    mvc.perform(get("/admin-camiones?camion=ac456ef"))
        .andExpect(
            content()
                .string(matchesPattern("(?s).*id=\"deshabilitar-ac456ef\"[^>]*data-bloquea.*")));
    mvc.perform(get("/admin-entidades?ver=pendientes&entidad=merendero-este"))
        .andExpect(
            content()
                .string(matchesPattern("(?s).*id=\"corregir-[a-z-]+\" popover[^>]*data-bloquea.*")))
        .andExpect(
            content()
                .string(
                    matchesPattern("(?s).*id=\"rechazar-[a-z-]+\" popover[^>]*data-bloquea.*")));
    mvc.perform(get("/admin-donaciones-detalle"))
        .andExpect(
            content().string(matchesPattern("(?s).*id=\"vencida\" popover[^>]*data-bloquea.*")));
  }

  @Test
  void cadaAccionDeNecesidadesLlevaASuAviso() throws Exception {
    mvc.perform(post("/entidad-necesidades-baja"))
        .andExpect(redirectedUrl("/entidad-necesidades-baja"));
    mvc.perform(post("/entidad-necesidades-publicada"))
        .andExpect(redirectedUrl("/entidad-necesidades-publicada"));
    mvc.perform(post("/entidad-necesidades-cambiada"))
        .andExpect(redirectedUrl("/entidad-necesidades-cambiada"));
    mvc.perform(get("/entidad-necesidades-baja"))
        .andExpect(content().string(containsString("Diste de baja Aceite de girasol.")))
        .andExpect(content().string(containsString("Cumplidas y terminadas (4)")));
    mvc.perform(get("/entidad-necesidades-publicada"))
        .andExpect(content().string(containsString("Publicamos tu necesidad.")))
        .andExpect(content().string(containsString("pedido--nuevo")))
        // hx-swap-oob="beforeend" suma a #avisos los hijos de su caja: el aviso va entero adentro.
        .andExpect(
            content()
                .string(
                    matchesPattern(
                        "(?s).*<div hx-swap-oob=\"beforeend:#avisos\"><div class=\"aviso-breve\"\\s+"
                            + "data-estado=\"hecho\" role=\"status\".*")));
    mvc.perform(get("/entidad-necesidades-cambiada"))
        .andExpect(content().string(containsString("Guardamos los cambios de Lentejas secas.")))
        .andExpect(content().string(containsString("Cambiada hoy")))
        .andExpect(content().string(containsString("Hoy pasó de 40 a 45 kg por mes.")))
        .andExpect(recibidos("10", "de 45 kg recibidos"));
  }

  @Test
  void lasNecesidadesSiguenLoDecididoConElGrupo() throws Exception {
    mvc.perform(get("/entidad-necesidades?necesidad=arroz"))
        .andExpect(content().string(containsString("Cada quincena")))
        .andExpect(content().string(containsString("Fideos secos (kg)")))
        .andExpect(content().string(containsString("Cuánto, en kg")))
        .andExpect(content().string(containsString("Vale desde este período, octubre")))
        .andExpect(content().string(containsString("1 entrega ya salió del depósito")))
        .andExpect(content().string(containsString("Martina Ruiz · 5 kg")));
    mvc.perform(get("/entidad-necesidades?necesidad=leche"))
        .andExpect(content().string(containsString("1 entrega todavía no salió")));
    mvc.perform(get("/entidad-necesidades?necesidad=ropa"))
        .andExpect(content().string(containsString("Donante Sostenedor · 5 unidades")))
        .andExpect(content().string(containsString("Ver los anteriores")))
        .andExpect(recibidos("0", "de 20 litros recibidos"))
        .andExpect(content().string(containsString("Hasta el 20 de octubre")))
        .andExpect(content().string(not(containsString("20 litros · hasta"))))
        .andExpect(content().string(containsString("Desde junio de 2026")));
    mvc.perform(get("/entidad-necesidades?necesidad=ropa&escenario=sin-categoria"))
        .andExpect(content().string(containsString("Un donante · 5 unidades")))
        .andExpect(content().string(not(containsString("null"))));
    mvc.perform(get("/entidad-seguimiento-confirmar?entrega=ropa&escenario=sin-categoria"))
        .andExpect(content().string(containsString(", de un donante.")));
    mvc.perform(get("/entidad-seguimiento-confirmar?entrega=ropa"))
        .andExpect(content().string(containsString("<strong>Ropa de abrigo, 10 unidades</strong>")))
        .andExpect(content().string(not(containsString("bultos"))));
  }

  @Test
  void elContactoLlegaConElCodigoYVuelveASuPantalla() throws Exception {
    mvc.perform(
            get("/publico-contacto")
                .param("codigo", "a1b2c3d4e5f6")
                .param("volver", "/entidad-necesidades"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("value=\"a1b2c3d4e5f6\"")))
        .andExpect(content().string(containsString("name=\"correo\"")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "action=\"/publico-contacto-enviado?volver=/entidad-necesidades\"")))
        .andExpect(content().string(containsString("Términos y privacidad")));
    mvc.perform(get("/publico-contacto?escenario=con-sesion"))
        .andExpect(content().string(containsString("el correo de tu cuenta")))
        .andExpect(content().string(not(containsString("name=\"correo\""))));
    mvc.perform(get("/publico-contacto-error"))
        .andExpect(content().string(containsString("Revisá dos cosas antes de enviar")))
        .andExpect(content().string(containsString("aria-invalid=\"true\"")));
    mvc.perform(
            post("/publico-contacto-enviado")
                .param("volver", "/entidad-necesidades")
                .param("mensaje", "x"))
        .andExpect(redirectedUrl("/publico-contacto-enviado?volver=%2Fentidad-necesidades"));
    mvc.perform(post("/publico-contacto-enviado").param("volver", "https://otro.test"))
        .andExpect(redirectedUrl("/publico-contacto-enviado"));
    for (String afuera : List.of("/../x", "//otro.test", "/\\otro.test", "/\t/otro.test", "/x")) {
      mvc.perform(post("/publico-contacto-enviado").param("volver", afuera))
          .andExpect(redirectedUrl("/publico-contacto-enviado"));
      mvc.perform(get("/publico-contacto-enviado").param("volver", afuera))
          .andExpect(content().string(containsString(RECIBIDO)))
          .andExpect(content().string(not(containsString("Volver a"))))
          .andExpect(content().string(not(containsString("otro.test"))));
    }
    mvc.perform(
            post("/publico-contacto-enviado")
                .param("volver", "/entidad-seguimiento-confirmar?entrega=lentejas"))
        .andExpect(
            redirectedUrl(
                "/publico-contacto-enviado?volver=%2Fentidad-seguimiento-confirmar%3Fentrega%3Dlentejas"));
    mvc.perform(get("/publico-contacto-enviado").param("volver", "/entidad-necesidades"))
        .andExpect(view().name("publico-contacto-enviado"))
        .andExpect(content().string(containsString(RECIBIDO)))
        .andExpect(content().string(containsString("name=\"mensaje\"")))
        .andExpect(
            content()
                .string(containsString("href=\"/entidad-necesidades\">Volver a Necesidades</a>")));
    mvc.perform(get("/publico-contacto-enviado").param("volver", "https://otro.test"))
        .andExpect(content().string(containsString(RECIBIDO)))
        .andExpect(content().string(not(containsString("otro.test"))));
    mvc.perform(get("/publico-contacto").param("volver", "/entidad-necesidades"))
        .andExpect(content().string(containsString("Volver a Necesidades")))
        .andExpect(content().string(not(containsString(RECIBIDO))));
  }

  /** El aviso breve de Contacto después de enviar, con el número de ejemplo (DA-06, DA-24). */
  private static final String RECIBIDO = "Recibimos tu mensaje. Su número es <b>4821</b>.";

  @Test
  void miPerfilEligeSiSeVeElNombreYMuestraLosLegales() throws Exception {
    mvc.perform(get("/donante-perfil"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("seccion", "Mi perfil"))
        .andExpect(
            content().string(containsString("href=\"/donante-perfil\" aria-current=\"page\"")))
        .andExpect(content().string(containsString("Donó: Distribuidora Ejemplo SA")))
        .andExpect(content().string(containsString("Guardar mi elección")))
        .andExpect(content().string(containsString("Aceptaste la versión 1")));
    mvc.perform(post("/donante-perfil-guardado").param("mostrar", "si"))
        .andExpect(redirectedUrl("/donante-perfil-guardado?mostrar=si"));
    mvc.perform(post("/donante-perfil-guardado").param("mostrar", "no"))
        .andExpect(redirectedUrl("/donante-perfil-guardado"));
    mvc.perform(get("/donante-perfil-guardado"))
        .andExpect(content().string(containsString("Las organizaciones ya no ven tu nombre")))
        .andExpect(content().string(containsString("ven «Empresa donante · Sostenedor»")))
        .andExpect(
            content().string(containsString("la organización ve tu categoría como donante")));
    mvc.perform(get("/donante-perfil-guardado?mostrar=si"))
        .andExpect(content().string(containsString("Las organizaciones ven tu nombre")));
    mvc.perform(post("/donante-perfil-guardado").param("seccion", "datos"))
        .andExpect(redirectedUrl("/donante-perfil-guardado?seccion=datos"));
    mvc.perform(post("/donante-perfil-guardado").param("seccion", "otra"))
        .andExpect(redirectedUrl("/donante-perfil-guardado"));
    mvc.perform(get("/donante-perfil-guardado?seccion=datos"))
        .andExpect(content().string(containsString("Guardamos los datos de tu cuenta")))
        .andExpect(content().string(not(containsString("Las organizaciones ya no ven tu nombre"))));
    mvc.perform(get("/donante-perfil-guardado?seccion=avisos"))
        .andExpect(content().string(containsString("Guardamos cómo te avisamos")));
    mvc.perform(get("/publico-legales"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("<p class=\"huerta-rotulo\">Legales</p>")))
        .andExpect(content().string(containsString("<a href=\"#datos\">Tus datos</a>")))
        .andExpect(content().string(containsString("Versión 1, vigente desde")))
        .andExpect(content().string(containsString("texto de ejemplo")))
        .andExpect(content().string(containsString("o solo en una donación al registrarla")))
        .andExpect(content().string(containsString("«Donante Sostenedor»")));
    mvc.perform(get("/entidad-seguimiento-sellada-ropa?escenario=sin-categoria"))
        .andExpect(content().string(containsString("Le avisamos a quien donó")));
    // La portada de A01 no tiene las abejas ni su pausa (DA-22).
    mvc.perform(get("/publico-portada"))
        .andExpect(content().string(containsString("href=\"/publico-legales\"")))
        .andExpect(content().string(not(containsString("id=\"pausar-movimiento\""))))
        .andExpect(content().string(not(containsString("/js/abejas.js"))));
  }

  @Test
  void llegoConUnProblemaConfirmaYReportaEnUnSoloEnvio() throws Exception {
    mvc.perform(get("/entidad-seguimiento-confirmar?entrega=lentejas"))
        .andExpect(
            content()
                .string(containsString("href=\"/entidad-seguimiento-problema?entrega=lentejas\"")));
    mvc.perform(get("/entidad-seguimiento-problema?entrega=lentejas"))
        .andExpect(status().isOk())
        .andExpect(view().name("entidad-seguimiento-problema"))
        .andExpect(model().attribute("seccion", "Entregas"))
        .andExpect(content().string(containsString("<h1>Llegó con un problema</h1>")))
        .andExpect(
            content().string(containsString("action=\"/entidad-seguimiento-sellada-lentejas\"")))
        .andExpect(content().string(containsString("value=\"incompleta\" required")))
        .andExpect(content().string(containsString("value=\"danada\"")))
        .andExpect(content().string(containsString("value=\"otro\"")))
        .andExpect(content().string(containsString("Cuánto llegó, en kg")))
        .andExpect(content().string(containsString("inputmode=\"decimal\"")))
        .andExpect(content().string(containsString("maxlength=\"500\"")))
        .andExpect(content().string(containsString("no se publican en la portada ni en el mapa")))
        .andExpect(content().string(containsString("Confirmar y contar el problema")))
        .andExpect(content().string(containsString("No hubo problema: volver a la confirmación")))
        .andExpect(content().string(not(containsString("entidad-seguimiento-problema?entrega"))));
    mvc.perform(get("/entidad-seguimiento-problema?entrega=ropa"))
        .andExpect(content().string(containsString("Cuánto llegó, en unidades")))
        .andExpect(content().string(containsString("step=\"1\"")))
        .andExpect(content().string(containsString("inputmode=\"numeric\"")));
  }

  @Test
  void llegoConUnProblemaMuestraCadaEstado() throws Exception {
    mvc.perform(get("/entidad-seguimiento-problema?escenario=revisar"))
        .andExpect(content().string(containsString("role=\"alert\"")))
        .andExpect(content().string(containsString("href=\"#cantidad\">Falta cuánto llegó")))
        .andExpect(content().string(containsString("Escribí cuánto llegó, en kg.")))
        .andExpect(
            content().string(containsString("aria-describedby=\"cantidad-error cantidad-ayuda\"")))
        .andExpect(content().string(containsString("Falta el número de remito.")));
    mvc.perform(get("/entidad-seguimiento-problema?escenario=error"))
        .andExpect(content().string(containsString("problema no quedó asentado")));
    mvc.perform(get("/entidad-seguimiento-problema?escenario=lento"))
        .andExpect(content().string(containsString("Confirmando…")));
    mvc.perform(get("/entidad-seguimiento-problema?escenario=confirmada"))
        .andExpect(content().string(containsString("Ya confirmaste esta entrega")));
    mvc.perform(get("/entidad-seguimiento-problema?escenario=vacio"))
        .andExpect(content().string(containsString("No encontramos esa entrega")));
  }

  @Test
  void elProblemaQuedaAsentadoAlConfirmar() throws Exception {
    mvc.perform(post("/entidad-seguimiento-sellada-lentejas").param("motivo", "incompleta"))
        .andExpect(redirectedUrl("/entidad-seguimiento-sellada-lentejas?problema=incompleta"));
    mvc.perform(post("/entidad-seguimiento-sellada-lentejas").param("motivo", "cualquiera"))
        .andExpect(redirectedUrl("/entidad-seguimiento-sellada-lentejas"));
    mvc.perform(get("/entidad-seguimiento-sellada-lentejas?problema=incompleta"))
        .andExpect(content().string(containsString("<dt>El problema</dt>")))
        .andExpect(content().string(containsString("Llegó menos: 12 de 15 kg.")))
        .andExpect(content().string(containsString("Dos bolsas rotas.")))
        .andExpect(content().string(containsString("con tus fotos, la fecha y el problema")))
        .andExpect(content().string(not(containsString("25 de 40 kg"))));
    mvc.perform(get("/entidad-seguimiento-sellada-ropa?problema=danada"))
        .andExpect(content().string(containsString("Llegó dañada.")));
    mvc.perform(get("/entidad-seguimiento-sellada-lentejas"))
        .andExpect(content().string(not(containsString("El problema"))))
        .andExpect(content().string(containsString("25 de 40 kg recibidos")))
        .andExpect(content().string(containsString("con tus fotos y la fecha.")));
  }

  @Test
  void enLaMaquetaUnaPantallaPuedeResponderSinPermisoOConUnaFalla() throws Exception {
    mvc.perform(get("/entidad-inicio?respuesta=403")).andExpect(status().isForbidden());
    mvc.perform(get("/donante-inicio?respuesta=429")).andExpect(status().isTooManyRequests());
    mvc.perform(get("/donante-inicio?respuesta=500")).andExpect(status().isInternalServerError());
    mvc.perform(get("/donante-inicio?respuesta=otra")).andExpect(status().isOk());
  }

  @Test
  void lasPaginasDeErrorDicenQuePasoYAQuienAvisar() throws Exception {
    String html = error(404, "/no-existe", "");
    assertTrue(html.contains("Error 404"), html);
    assertTrue(html.contains("No encontramos esta"));
    assertTrue(html.contains("Código para soporte: <code>" + CODIGO + "</code>"), html);
    assertTrue(html.contains("href=\"/publico-contacto?codigo=" + CODIGO + "\""));
    assertTrue(html.contains("class=\"raices-publico\""));
    html = error(403, "/entidad-inicio", "");
    assertTrue(html.contains("Esta página no es para tu"));
    assertTrue(html.contains("<title>Sin permiso - DonaTrack</title>"));
    html = error(500, "/publico-mapa", "");
    assertTrue(html.contains("Error 500"));
    assertTrue(html.contains("href=\"/publico-mapa\">Volver a intentar"), html);
    assertTrue(html.contains("href=\"/\">Ir al inicio</a>"), html);
    html = error(500, "/publico-mapa", "sin-codigo");
    assertTrue(!html.contains("Código para soporte"));
    assertTrue(html.contains("Si sigue, escribinos y contanos qué estabas haciendo."));
    // El rate limiting del gateway (E7): esperar y volver a intentar la misma dirección.
    html = error(429, "/publico-mapa", "");
    assertTrue(html.contains("Error 429"), html);
    assertTrue(html.contains("<title>Demasiados pedidos seguidos - DonaTrack</title>"), html);
    assertTrue(html.contains("Esperá un minuto y probá de nuevo."), html);
    assertTrue(html.contains("href=\"/publico-mapa\">Volver a intentar"), html);
    assertTrue(html.contains("Código para soporte: <code>" + CODIGO + "</code>"), html);
  }

  @Test
  void conSesionLasPaginasDeErrorUsanElMarcoPrivadoYOfrecenVolverAMiInicio() throws Exception {
    // El área de la maqueta sale de la dirección pedida; la de la cuenta real, del token.
    String html = error(403, "/admin-donantes", "");
    assertTrue(html.contains("class=\"raices-privado\""), html);
    assertTrue(html.contains("Esta página no es para tu"), html);
    assertTrue(html.contains("Cerrar sesión"), html);
    assertTrue(!html.contains("Crear una cuenta"), html);
    assertTrue(html.contains("href=\"/admin-inicio\">Volver a mi inicio</a>"), html);
    assertTrue(!html.contains("aria-current"), html);
    assertTrue(html.contains("href=\"/publico-contacto?codigo=" + CODIGO + "\""), html);

    html = error(404, "/entidad-tesoro", "");
    assertTrue(html.contains("href=\"/entidad-inicio\">Volver a mi inicio</a>"), html);
    assertTrue(html.contains("Necesidades"), html);

    html = error(500, "/donante-inicio", "");
    assertTrue(html.contains("href=\"/donante-inicio\">Volver a intentar"), html);
    assertTrue(html.contains("href=\"/donante-inicio\">Volver a mi inicio</a>"), html);
    assertTrue(!html.contains("Ir al inicio"), html);
    assertTrue(html.indexOf("Volver a intentar") < html.indexOf("Volver a mi inicio"), html);
    assertTrue(html.contains("aria-label=\"Notificaciones\""), html);
    assertTrue(!html.contains("armazon-avisos-cuenta"), html);
  }

  @Test
  void unaDireccionPublicaOQueNoExisteDaLaPaginaDeErrorPublica() throws Exception {
    for (String direccion : List.of("/publico-mapa", "/no-existe", "/")) {
      String html = error(404, direccion, "");
      assertTrue(html.contains("class=\"raices-publico\""), html);
      assertTrue(html.contains("href=\"/\">Ir al inicio</a>"), html);
      assertTrue(!html.contains("Volver a mi inicio"), html);
    }
  }

  @Test
  void conSesionContactoUsaElMarcoPrivadoDelRol() throws Exception {
    mvc.perform(get("/publico-contacto?escenario=con-sesion"))
        .andExpect(content().string(containsString("class=\"raices-privado\"")))
        .andExpect(content().string(containsString("Cerrar sesión")))
        .andExpect(content().string(not(containsString("Crear una cuenta"))))
        .andExpect(content().string(not(containsString("aria-current"))))
        .andExpect(content().string(containsString("href=\"/donante-donaciones\"")));
    mvc.perform(get("/publico-contacto?escenario=con-sesion&area=entidad"))
        .andExpect(content().string(containsString("href=\"/entidad-necesidades\"")));
    mvc.perform(get("/publico-contacto?escenario=con-sesion&area=admin"))
        .andExpect(content().string(containsString("href=\"/admin-ranking\"")));
    mvc.perform(get("/publico-contacto"))
        .andExpect(content().string(containsString("class=\"raices-publico\"")))
        .andExpect(content().string(not(containsString("Cerrar sesión"))));
  }

  /**
   * El envío termina en Contacto con el aviso breve, sin página de respuesta propia (DA-06, DA-24):
   * con la sesión, en el marco privado del rol; la única salida propia es la de ?volver=.
   */
  @Test
  void elEnviadoEsContactoConElAvisoConYSinSesion() throws Exception {
    mvc.perform(get("/publico-contacto-enviado?escenario=con-sesion&area=entidad"))
        .andExpect(content().string(containsString("class=\"raices-privado\"")))
        .andExpect(content().string(containsString("href=\"/entidad-necesidades\"")))
        .andExpect(content().string(containsString(RECIBIDO)))
        .andExpect(content().string(containsString("el correo de tu cuenta")))
        .andExpect(content().string(not(containsString("Volver a"))));
    mvc.perform(
            get("/publico-contacto-enviado?escenario=con-sesion")
                .param("volver", "/donante-seguimiento"))
        .andExpect(content().string(containsString(RECIBIDO)))
        .andExpect(content().string(containsString("Volver a Seguimiento")));
    mvc.perform(get("/publico-contacto-enviado"))
        .andExpect(content().string(containsString("class=\"raices-publico\"")))
        .andExpect(content().string(containsString(RECIBIDO)))
        .andExpect(content().string(not(containsString("Volver a"))));
  }

  /** El traceId de ejemplo que TraceIdFiltro habría dejado en el pedido. */
  private static final String CODIGO = "0123456789abcdef0123456789abcdef";

  /** La página de error que arma Spring Boot para ese código, como después de un sendError. */
  private String error(int codigo, String direccion, String escenario) throws Exception {
    return mvc.perform(
            get("/error")
                .param("escenario", escenario)
                .accept(MediaType.TEXT_HTML)
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, codigo)
                .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, direccion)
                // Lo deja TraceIdFiltro en el pedido original; el despacho de error lo hereda.
                .requestAttr(TraceIdFiltro.CLAVE, CODIGO))
        .andExpect(status().is(codigo))
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  @Test
  void unFormularioVuelveASuPantalla() throws Exception {
    mvc.perform(post("/donante-donaciones-prueba"))
        .andExpect(redirectedUrl("/donante-donaciones-prueba"));
  }

  @Test
  void laPortadaEnlazaAlSitio() throws Exception {
    // El Mapa ya no va en la portada ni en el menú: queda dentro de Entregas (DA-01).
    mvc.perform(get("/publico-portada"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("href=\"/publico-ingresar\"")))
        .andExpect(content().string(containsString("href=\"/publico-donaciones\">Ver todas</a>")))
        .andExpect(content().string(containsString("href=\"/publico-registro-donante\"")))
        .andExpect(content().string(containsString("href=\"/publico-registro-entidad\"")))
        .andExpect(content().string(not(containsString("href=\"/prototipo/"))));
  }

  /**
   * El menú público de la guía (DA-01, DA-05 y DA-17): Inicio, Entregas, Organizaciones y Cómo
   * funciona (Nosotros), en ese orden y sin el Mapa, que sigue respondiendo y marca Entregas.
   */
  @Test
  void elMenuPublicoEsInicioEntregasOrganizacionesYComoFunciona() throws Exception {
    String html =
        mvc.perform(get("/publico-portada"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    String barra =
        html.substring(
            html.indexOf("class=\"navegacion-barra\""),
            html.indexOf("class=\"navegacion-acceso\""));
    assertTrue(
        barra.matches(
            "(?s).*href=\"/publico-portada\" aria-current=\"page\">.*Inicio</a>"
                + ".*href=\"/publico-donaciones\">.*Entregas</a>"
                + ".*href=\"/publico-entidades\">.*Organizaciones</a>"
                + ".*href=\"/publico-nosotros\">.*Cómo funciona</a>.*"),
        barra);
    assertFalse(barra.contains("/publico-mapa"), barra);
    assertFalse(barra.contains(">Mapa<"), barra);
    assertTrue(barra.contains("<use href=\"#i-brote\">"), barra);
    assertTrue(html.contains("<symbol id=\"i-brote\""), "El brote de Cómo funciona");
    mvc.perform(get("/publico-mapa"))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString("href=\"/publico-mapa\" aria-current"))))
        .andExpect(
            content().string(containsString("href=\"/publico-donaciones\" aria-current=\"page\"")));
    mvc.perform(get("/publico-donaciones"))
        .andExpect(
            content().string(containsString("href=\"/publico-donaciones\" aria-current=\"page\"")));
    mvc.perform(get("/publico-entidades"))
        .andExpect(
            content().string(containsString("href=\"/publico-entidades\" aria-current=\"page\"")));
    for (String conocer : List.of("/publico-nosotros", "/publico-para-donantes")) {
      mvc.perform(get(conocer))
          .andExpect(
              content().string(containsString("href=\"/publico-nosotros\" aria-current=\"page\"")));
    }
  }

  /**
   * La portada sigue a A01 (PUB1-01): la última entrega confirmada con su etiqueta, un fruto por
   * entrega del mes, las cifras, la raíz de 5 pasos y las del mes; quien recibió es la organización
   * (HG-03, DA-10). Cada estado, con su texto.
   */
  @Test
  void laPortadaSigueAlArquetipoConSusEstados() throws Exception {
    String html =
        mvc.perform(get("/publico-portada"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    assertTrue(html.contains("class=\"trama\""), "el título con la trama");
    assertTrue(
        html.contains("Seguí tu donación hasta que la organización la confirma"), "el título");
    assertTrue(html.contains(">Quiero donar</a>"), "la acción llena");
    assertTrue(html.contains(">Soy una organización</a>"), "la otra acción");
    assertEquals(1, html.split("data-tono=\"principal\"", -1).length - 1, "una sola acción llena");
    assertTrue(html.contains("ilustracion-recorrido.svg#ilustracion-recorrido"), "el dibujo");
    // La maqueta trae 10 entregas confirmadas en el último mes: 10 frutos.
    String frutos =
        html.substring(
            html.indexOf("class=\"huerta-frutos\""),
            html.indexOf("</ul>", html.indexOf("class=\"huerta-frutos\"")));
    assertEquals(10, frutos.split("<li>", -1).length - 1, frutos);
    assertTrue(html.contains("10 entregas confirmadas"), "la cuenta de los frutos");
    assertTrue(html.contains("etiqueta-entrega--costado"), "la última, con la foto al costado");
    assertTrue(
        html.contains("Recibió: Escuela Ejemplo N.º 12, el 14 de octubre."), "quién recibió");
    assertTrue(html.contains("Cómo sabemos que llega"), "las cifras");
    assertTrue(
        html.contains(
            "Hasta hoy, <strong>1.284</strong> entregas confirmadas, <strong>46</strong>"
                + " organizaciones en el mapa y <strong>32</strong> donaciones en camino ahora."),
        "las cifras, en una frase como en A01");
    assertFalse(html.contains("class=\"datos\""), "sin la lista de datos");
    assertTrue(html.contains("data-forma=\"numerada tendida\""), "la raíz numerada");
    assertEquals(5, html.split("class=\"huerta-raiz-nudo\"", -1).length - 1, "los 5 pasos");
    assertEquals(
        4, html.split("<article class=\"etiqueta-entrega", -1).length - 1, "la última y 3 más");
    assertFalse(html.contains("huerta-hero"), "sin el hero de K");
    assertFalse(html.contains("huerta-tierra"), "sin el corte bajo tierra");
    assertFalse(html.contains("huerta-cierre"), "sin el cierre de K");
    mvc.perform(get("/publico-portada?escenario=vacio"))
        .andExpect(status().isOk())
        .andExpect(
            content().string(containsString("Todavía no hay entregas confirmadas este mes.")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "class=\"vacio\" data-forma=\"filetes\">Todavía no hay entregas")))
        .andExpect(content().string(not(containsString("<strong>1.284</strong>"))))
        .andExpect(content().string(not(containsString("class=\"etiqueta-entrega"))));
    String lento =
        mvc.perform(get("/publico-portada?escenario=lento"))
            .andExpect(
                content()
                    .string(containsString("role=\"status\">Cargando las entregas confirmadas…")))
            .andExpect(content().string(containsString("role=\"status\">Cargando las cifras…")))
            .andReturn()
            .getResponse()
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    int cifras = lento.indexOf("aria-labelledby=\"portada-cifras\"");
    assertTrue(
        cifras > 0
            && lento.substring(cifras, lento.indexOf('>', cifras)).contains("aria-busy=\"true\""),
        "la sección de las cifras, ocupada mientras carga");
    mvc.perform(get("/publico-portada?escenario=error"))
        .andExpect(content().string(containsString("role=\"alert\"")))
        .andExpect(content().string(containsString("No pudimos cargar las entregas confirmadas.")))
        .andExpect(content().string(containsString("data-forma=\"filetes\" role=\"alert\"")))
        .andExpect(content().string(containsString("No pudimos cargar las cifras.")))
        .andExpect(content().string(not(containsString("<strong>1.284</strong>"))));
  }

  /**
   * F6: Nosotros y las dos landings se enlazan desde el pie de todas las páginas y desde la
   * portada; las cifras de Nosotros llevan período y fecha, y cada estado su aviso (ficha
   * raices-conocer-y-activar.md).
   */
  @Test
  void nosotrosYLasLandingsSeEnlazanDesdeElPieYLaPortada() throws Exception {
    for (String pantalla : List.of("/publico-portada", "/publico-legales", "/donante-inicio")) {
      mvc.perform(get(pantalla))
          .andExpect(content().string(containsString("href=\"/publico-nosotros\"")))
          .andExpect(content().string(containsString("href=\"/publico-para-donantes\"")))
          .andExpect(content().string(containsString("href=\"/publico-para-entidades\"")));
    }
    mvc.perform(get("/publico-portada"))
        // Las dos acciones de A01, cada una a su registro, y Cómo funciona en el menú (DA-05).
        .andExpect(
            content().string(containsString("href=\"/publico-registro-donante\">Quiero donar</a>")))
        .andExpect(
            content()
                .string(
                    containsString("href=\"/publico-registro-entidad\">Soy una organización</a>")))
        .andExpect(content().string(containsString("href=\"/publico-nosotros\">")));
    mvc.perform(get("/publico-nosotros"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Entregas confirmadas desde marzo de 2026")))
        // Las cifras en árboles de K (2026-11-08; antes, la rama de frutos con data-cosecha).
        .andExpect(content().string(containsString("class=\"huerta-cifras\"")))
        .andExpect(content().string(containsString("Actualizado")))
        // Nosotros es Cómo funciona en el menú público, y lo marca (DA-05).
        .andExpect(
            content().string(containsString("href=\"/publico-nosotros\" aria-current=\"page\"")));
    mvc.perform(get("/publico-nosotros?escenario=vacio"))
        .andExpect(content().string(containsString("Todavía no hay entregas confirmadas.")))
        .andExpect(content().string(not(containsString("Actualizado"))));
    mvc.perform(get("/publico-nosotros?escenario=lento"))
        .andExpect(content().string(containsString("role=\"status\">Cargando las cifras…")));
    mvc.perform(get("/publico-nosotros?escenario=error"))
        .andExpect(content().string(containsString("No pudimos cargar las cifras.")));
    mvc.perform(get("/publico-para-donantes"))
        .andExpect(content().string(containsString("href=\"/publico-registro-donante\"")))
        .andExpect(content().string(containsString("<details>")));
    mvc.perform(get("/publico-para-entidades"))
        .andExpect(content().string(containsString("href=\"/publico-registro-entidad\"")))
        .andExpect(content().string(not(containsString("beneficiario"))));
  }

  /**
   * Ingresar (PUB1-06) deriva de A12 de la guía: la columna del formulario con los campos del
   * catálogo, una sola acción llena y las cuentas de ejemplo de la demo, en una lámina punteada.
   */
  @Test
  void ingresarEsLaColumnaDelFormularioConLasCuentasDeEjemplo() throws Exception {
    String html =
        mvc.perform(get("/publico-ingresar"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("<h1>Ingresar</h1>")))
            .andExpect(content().string(containsString("action=\"/donante-inicio\"")))
            .andExpect(content().string(containsString("method=\"post\"")))
            .andExpect(content().string(containsString("class=\"campo\"")))
            .andExpect(content().string(containsString("autocomplete=\"current-password\"")))
            .andExpect(content().string(containsString("href=\"/publico-recuperar\"")))
            .andExpect(content().string(containsString("href=\"/publico-registro\"")))
            .andExpect(content().string(containsString("Continuar con Google")))
            .andExpect(content().string(containsString("class=\"tarjeta\" data-papel=\"hueco\"")))
            .andExpect(content().string(containsString("Cuentas de ejemplo")))
            .andExpect(content().string(containsString("href=\"/donante-inicio?cuenta=empresa\"")))
            .andExpect(
                content().string(containsString("Distribuidora Ejemplo SA, empresa donante")))
            .andExpect(content().string(containsString("href=\"/entidad-inicio\"")))
            .andExpect(content().string(containsString("Comedor Barrio Ejemplo, organización")))
            .andExpect(content().string(containsString("href=\"/admin-inicio\"")))
            .andExpect(content().string(not(containsString("aria-invalid"))))
            .andExpect(content().string(not(containsString("role=\"alert\""))))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertEquals(1, apariciones(html, "data-tono=\"principal\""), "una sola acción llena");
    mvc.perform(get("/publico-ingresar?escenario=lento"))
        .andExpect(content().string(containsString("aria-busy=\"true\"")))
        .andExpect(content().string(containsString("role=\"status\">Ingresando…")));
    mvc.perform(get("/publico-ingresar?escenario=error"))
        .andExpect(content().string(containsString("No pudimos ingresar en este momento.")))
        .andExpect(content().string(containsString("role=\"alert\"")));
    mvc.perform(get("/publico-ingresar?escenario=sin-conexion"))
        .andExpect(content().string(containsString("Se cortó la conexión.")))
        .andExpect(content().string(containsString("role=\"alert\"")));
    mvc.perform(get("/publico-ingresar?escenario=bloqueado"))
        .andExpect(content().string(containsString("Pausamos los intentos de ingreso.")))
        .andExpect(content().string(containsString("role=\"alert\"")));
    mvc.perform(get("/publico-ingresar?escenario=pendiente"))
        .andExpect(content().string(containsString("Tu organización todavía está en revisión.")))
        .andExpect(content().string(containsString("role=\"status\"")));
    mvc.perform(get("/publico-ingresar?escenario=sesion-vencida"))
        .andExpect(content().string(containsString("Tu sesión se cerró.")))
        .andExpect(content().string(containsString("role=\"status\"")));
  }

  /**
   * El error de Ingresar es un estado del mismo formulario (A12, «Con error»): el resumen arriba,
   * con el foco y un enlace al campo; los dos campos marcados en su lugar y un solo mensaje, sin
   * decir cuál de los dos falló (RF-04, RNF-AUTH-10). Lo escrito en el correo vuelve.
   */
  @Test
  void elErrorDeIngresarMarcaLosCamposSinDecirCualFallo() throws Exception {
    String html =
        mvc.perform(get("/publico-ingresar-error"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("<title>No pudimos ingresar - DonaTrack")))
            .andExpect(content().string(containsString("class=\"aviso\" data-tipo=\"error\"")))
            .andExpect(content().string(containsString("role=\"alert\" tabindex=\"-1\"")))
            .andExpect(content().string(containsString("autofocus")))
            .andExpect(content().string(containsString("href=\"#clave\"")))
            .andExpect(content().string(containsString("El correo o la contraseña no coinciden")))
            .andExpect(content().string(containsString("class=\"campo-error\"")))
            .andExpect(
                content()
                    .string(
                        matchesPattern(
                            "(?s).*id=\"ingreso-error\" class=\"campo-error\">\\s*<svg[^>]*"
                                + "data-signo=\"e-problema\".*")))
            .andExpect(content().string(containsString("aria-describedby=\"ingreso-error\"")))
            .andExpect(content().string(not(containsString("Al menos 8 caracteres"))))
            .andExpect(
                content().string(containsString("value=\"contacto@distribuidora-ejemplo.test\"")))
            .andExpect(content().string(containsString("href=\"/publico-recuperar\"")))
            .andExpect(content().string(containsString("Cuentas de ejemplo")))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertEquals(2, apariciones(html, "data-estado=\"error\""), "los dos campos, marcados");
    assertEquals(2, apariciones(html, "aria-invalid=\"true\""), "los dos controles, inválidos");
    assertEquals(1, apariciones(html, "class=\"campo-error\""), "un solo mensaje");
    assertEquals(1, apariciones(html, "data-tono=\"principal\""), "una sola acción llena");
  }

  private static int apariciones(String texto, String buscado) {
    int cuenta = 0;
    for (int i = texto.indexOf(buscado); i >= 0; i = texto.indexOf(buscado, i + 1)) {
      cuenta++;
    }
    return cuenta;
  }

  /**
   * Activar cuenta: elegir la contraseña con un formulario real, el error de datos, el enlace
   * vencido con su remedio y el pedido de otro enlace, que no revela cuentas (RNF-AUTH-10).
   */
  @Test
  void activarLaCuentaEligeLaContrasenaYPideOtroEnlaceSinRevelarCuentas() throws Exception {
    String lista = "/publico-activar-lista";
    String pedido = "/publico-activar-pedido";
    mvc.perform(get("/publico-activar"))
        .andExpect(content().string(containsString("action=\"/publico-activar-lista\"")))
        .andExpect(content().string(containsString("method=\"post\"")))
        .andExpect(content().string(containsString("minlength=\"8\"")))
        .andExpect(content().string(containsString("readonly")));
    mvc.perform(post(lista)).andExpect(redirectedUrl(lista));
    mvc.perform(get(lista))
        .andExpect(content().string(containsString("Tu cuenta <em>está lista</em>")))
        .andExpect(content().string(containsString("href=\"/publico-ingresar\"")));
    mvc.perform(get("/publico-activar-error"))
        .andExpect(content().string(containsString("Revisá la contraseña")))
        .andExpect(content().string(containsString("aria-invalid=\"true\"")));
    mvc.perform(get("/publico-activar?escenario=vencido"))
        .andExpect(content().string(containsString("Este enlace ya no sirve.")))
        .andExpect(content().string(containsString("Activá <em>tu cuenta</em>")))
        .andExpect(content().string(containsString("href=\"/publico-activar-pedir\"")))
        .andExpect(content().string(not(containsString("action=\"/publico-activar-lista\""))));
    mvc.perform(get("/publico-activar?escenario=error"))
        .andExpect(content().string(containsString("No pudimos activar tu cuenta.")));
    mvc.perform(get("/publico-activar?escenario=sin-conexion"))
        .andExpect(content().string(containsString("No sabemos si tu cuenta quedó activada.")));
    mvc.perform(get("/publico-activar?escenario=lento"))
        .andExpect(content().string(containsString("role=\"status\">Activando…")));
    mvc.perform(get("/publico-ingresar"))
        .andExpect(content().string(containsString("href=\"/publico-activar-pedir\"")));
    mvc.perform(post("/publico-activar-pedido"))
        .andExpect(redirectedUrl("/publico-activar-pedido"));
    mvc.perform(get(pedido))
        .andExpect(
            content().string(containsString("Si hay una cuenta con ese correo, te mandamos")));
    mvc.perform(get("/publico-activar-pedir?escenario=error"))
        .andExpect(content().string(containsString("No pudimos mandar el enlace.")));
    mvc.perform(get("/publico-activar-pedir?escenario=sin-conexion"))
        .andExpect(content().string(containsString("No sabemos si el enlace salió.")));
  }

  /** Recuperar la contraseña: el aviso no revela cuentas ni promete un plazo (RNF-AUTH-10, D8). */
  @Test
  void recuperarLaContrasenaAvisaSinRevelarCuentas() throws Exception {
    String enviado = "/publico-recuperar-enviado";
    mvc.perform(post(enviado)).andExpect(redirectedUrl(enviado));
    mvc.perform(get(enviado))
        .andExpect(status().isOk())
        .andExpect(
            content().string(containsString("Si hay una cuenta con ese correo, te mandamos")))
        .andExpect(content().string(not(containsString("vence en"))));
  }

  /**
   * El acceso a Notificaciones va en todas las privadas del donante y la entidad, también las
   * aprobadas antes de la corrida A/B (pulido, 2026-11-03). Contacto y Legales llevan el menú
   * público, sin sección marcada (pulido, 2026-11-03, usuario).
   */
  @Test
  void elAccesoANotificacionesVaEnTodasLasPrivadas() throws Exception {
    for (String pantalla :
        List.of(
            "donante-perfil",
            "donante-seguimiento",
            "entidad-necesidades",
            "entidad-seguimiento-confirmar")) {
      mvc.perform(get("/" + pantalla))
          .andExpect(status().isOk())
          .andExpect(content().string(containsString("armazon-avisos")));
    }
    for (String pantalla : List.of("publico-contacto", "publico-legales")) {
      mvc.perform(get("/" + pantalla))
          .andExpect(status().isOk())
          .andExpect(content().string(containsString("data-zona=\"publica\"")))
          .andExpect(content().string(not(containsString("aria-current=\"page\""))));
    }
    mvc.perform(get("/donante-donaciones"))
        .andExpect(content().string(containsString("armazon-avisos")));
    mvc.perform(get("/publico-mapa"))
        .andExpect(content().string(containsString("data-zona=\"publica\"")));
  }

  /** Solo el menú de más de 5 secciones (la administración) lleva "Más" en el celular. */
  @Test
  void soloLaAdministracionLlevaElMenuCompleto() throws Exception {
    mvc.perform(get("/admin-inicio"))
        .andExpect(content().string(containsString("popovertarget=\"menu-completo\"")));
    mvc.perform(get("/donante-inicio"))
        .andExpect(content().string(not(containsString("menu-completo"))));
  }

  /** Las pantallas dibujadas de verdad (no caen en "sin dibujar"). */
  private static final List<String> DIBUJADAS =
      List.of(
          "publico-portada",
          "publico-donaciones",
          "publico-mapa",
          "publico-entidades",
          "publico-ingresar",
          "publico-registro",
          "publico-registro-donante",
          "publico-registro-entidad",
          "publico-contacto",
          "publico-legales",
          "publico-nosotros",
          "publico-para-donantes",
          "publico-para-entidades",
          "publico-activar",
          "publico-activar-pedir",
          "donante-inicio",
          "donante-donaciones",
          "donante-donaciones-detalle",
          "donante-entidades",
          "donante-notificaciones",
          "donante-seguimiento",
          "donante-perfil",
          "donante-misiones",
          "entidad-inicio",
          "entidad-seguimiento",
          "entidad-perfil",
          "entidad-entrega-problema",
          "entidad-notificaciones",
          "entidad-necesidades",
          "entidad-seguimiento-confirmar",
          "admin-inicio",
          "admin-donantes",
          "admin-entidades",
          "admin-donaciones",
          "admin-donaciones-registrar",
          "admin-asignaciones",
          "admin-camiones",
          "admin-ranking");

  @Test
  void losDonantesDelAdminMuestranCadaEstadoEImportanSinBloquear() throws Exception {
    mvc.perform(get("/admin-donantes"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("seccion", "Donantes"))
        .andExpect(
            content().string(containsString("86 donantes activos, al 15 de octubre de 2026")))
        // R11-34: una bandeja; la ficha del primero es la de partida y lleva a sus donaciones
        .andExpect(content().string(containsString("class=\"maestro\"")))
        .andExpect(content().string(containsString("data-elegido=\"partida\"")))
        // El primero no tiene donaciones: la ficha lo dice y no ofrece verlas (ficha §4.1)
        .andExpect(content().string(containsString("Todavía no tiene donaciones registradas.")))
        .andExpect(content().string(not(containsString("Sus donaciones · 0"))))
        .andExpect(content().string(containsString("Cerrar la ficha")))
        .andExpect(
            content().string(containsString("href=\"/admin-donantes?importar=abrir#importar\"")));
    // Importar queda plegado; "Importar CSV" lo abre sin dejar la lista (RF-20).
    // Ana tiene donaciones de ejemplo: el total, las más recientes y el enlace con el número
    mvc.perform(get("/admin-donantes?donante=ana"))
        .andExpect(content().string(containsString("Sus donaciones · ")))
        .andExpect(content().string(containsString("Ver sus ")))
        .andExpect(content().string(containsString("Correo:")));
    mvc.perform(get("/admin-donantes?donante=juan"))
        .andExpect(content().string(containsString("aria-current=\"true\"")))
        .andExpect(content().string(containsString("href=\"/admin-donaciones?donante=juan\"")))
        .andExpect(content().string(not(containsString("data-elegido=\"partida\""))));
    mvc.perform(get("/admin-donantes?importar=abrir"))
        .andExpect(content().string(containsString("enctype=\"multipart/form-data\"")))
        .andExpect(content().string(containsString("action=\"/admin-donantes-importando\"")))
        .andExpect(content().string(containsString("class=\"maestro\"")));
    mvc.perform(get("/admin-donantes?escenario=vacio"))
        .andExpect(content().string(containsString("Todavía no hay donantes registrados.")));
    mvc.perform(get("/admin-donantes?escenario=lento"))
        .andExpect(content().string(containsString("aria-busy=\"true\"")));
    mvc.perform(get("/admin-donantes?escenario=error"))
        .andExpect(content().string(containsString("No pudimos cargar los donantes.")))
        .andExpect(content().string(containsString("role=\"alert\"")));
    // R11-01: la lista llega de a una página (en la maqueta, de a 5), con su paginador.
    mvc.perform(get("/admin-donantes"))
        .andExpect(content().string(containsString("Página 1 de 2")))
        .andExpect(content().string(containsString("href=\"/admin-donantes?pagina=2\"")));
    mvc.perform(get("/admin-donantes?pagina=2"))
        .andExpect(content().string(containsString("Página 2 de 2")));
    // R11-02: el avance no carga la lista, se actualiza solo y dice los últimos importados.
    mvc.perform(get("/admin-donantes-importando"))
        .andExpect(content().string(containsString("8.400 de 20.000 filas")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "Donantes nuevos: 7.950 · Actualizados: 410 · Filas con error: 40")))
        .andExpect(content().string(containsString("Últimos importados")))
        .andExpect(content().string(containsString("every 3s")))
        .andExpect(content().string(not(containsString("Todos los donantes"))));
    mvc.perform(get("/admin-donantes-importando?escenario=esperando"))
        .andExpect(content().string(containsString("Todavía no empezó a importarse")));
    mvc.perform(get("/admin-donantes-importando?escenario=leyendo"))
        .andExpect(content().string(containsString("filas por ahora.")));
    mvc.perform(get("/admin-donantes-importado?escenario=cortada"))
        .andExpect(content().string(containsString("La importación se cortó en la fila 8.400.")));
    mvc.perform(get("/admin-donantes-importado"))
        .andExpect(content().string(containsString("19.100 donantes nuevos, 774 actualizados")))
        .andExpect(content().string(containsString("Falta el apellido.")))
        .andExpect(content().string(containsString("Página 1 de 7")))
        .andExpect(content().string(not(containsString("every 3s"))));
    mvc.perform(get("/admin-donantes-importado?escenario=sin-errores"))
        .andExpect(content().string(containsString("Todas las filas se importaron sin errores.")));
    mvc.perform(get("/admin-donantes-importar-error"))
        .andExpect(content().string(containsString("No se importó ninguna fila.")));
    mvc.perform(post("/admin-donantes-importando"))
        .andExpect(redirectedUrl("/admin-donantes-importando"));
  }

  @Test
  void registrarDonanteLlevaASuDonacionYNoDuplica() throws Exception {
    mvc.perform(get("/admin-donantes-registrar"))
        .andExpect(model().attribute("seccion", "Donantes"))
        .andExpect(content().string(containsString("Apellido *")))
        .andExpect(content().string(containsString("action=\"/admin-donantes-registrado\"")));
    mvc.perform(get("/admin-donantes-registrar?tipo=organizacion"))
        .andExpect(content().string(containsString("Razón social *")))
        .andExpect(content().string(not(containsString("Apellido *"))));
    mvc.perform(get("/admin-donantes-registrar-error"))
        .andExpect(content().string(containsString("Revisá 2 datos para seguir.")))
        .andExpect(content().string(containsString("aria-invalid=\"true\"")));
    mvc.perform(post("/admin-donantes-registrado"))
        .andExpect(redirectedUrl("/admin-donantes-registrado"));
    mvc.perform(get("/admin-donantes-registrado"))
        .andExpect(content().string(containsString("Registraste a Ana Ejemplo como donante.")))
        .andExpect(
            content()
                .string(
                    containsString("href=\"/admin-donaciones-registrar?donante=ana-ejemplo\"")));
    mvc.perform(get("/admin-donantes-registrado?escenario=ya-era"))
        .andExpect(
            content().string(containsString("Ana Ejemplo ya era donante. No cambiamos nada.")));
  }

  @Test
  void lasEntidadesDelAdminSeApruebanCorrigenORechazan() throws Exception {
    // Una sola lista, de la A a la Z; el estado es un filtro al lado de la búsqueda.
    mvc.perform(get("/admin-entidades?entidad=comedor"))
        .andExpect(model().attribute("seccion", "Organizaciones"))
        .andExpect(content().string(containsString("Activas · 46")))
        .andExpect(content().string(containsString("Pendientes · 3")))
        .andExpect(content().string(containsString("Inactivas · 3")))
        .andExpect(content().string(containsString("Activa desde el 4 de febrero")));
    mvc.perform(get("/admin-entidades?ver=pendientes&entidad=merendero-este"))
        .andExpect(
            content().string(containsString("aria-label=\"Aprobar: Merendero Ejemplo Este\"")))
        .andExpect(content().string(containsString("popover")))
        .andExpect(content().string(matchesPattern("(?s).*Falta la constancia de\\s+domicilio.*")));
    mvc.perform(get("/admin-entidades?ver=inactivas"))
        .andExpect(content().string(containsString("Inactiva desde el 20 de agosto")));
    mvc.perform(get("/admin-entidades?escenario=vacio&ver=pendientes"))
        .andExpect(content().string(containsString("Pendientes · 0")))
        .andExpect(
            content().string(containsString("Ninguna organización pide sumarse por ahora.")));
    mvc.perform(get("/admin-entidades?escenario=error"))
        .andExpect(content().string(containsString("No pudimos cargar las organizaciones.")));
    mvc.perform(get("/admin-entidades-aprobada"))
        .andExpect(content().string(containsString("Aprobaste a Hogar Ejemplo Norte.")))
        .andExpect(content().string(containsString("Pendientes · 2")))
        .andExpect(content().string(containsString("Activas · 47")))
        .andExpect(content().string(containsString("Activa desde el 15 de octubre")));
    mvc.perform(get("/admin-entidades-correccion"))
        .andExpect(content().string(containsString("Esperando")))
        .andExpect(content().string(containsString("Le pedimos a Merendero Ejemplo Este")))
        .andExpect(
            content().string(containsString("acciones vuelven cuando la organización la reenvíe.")))
        .andExpect(content().string(not(containsString("Aprobar: Merendero Ejemplo Este"))));
    mvc.perform(get("/admin-entidades-rechazada"))
        .andExpect(
            content().string(containsString("Rechazaste la solicitud de Comedor Ejemplo Oeste.")))
        .andExpect(content().string(not(containsString("Comedor Ejemplo Oeste</b>"))))
        .andExpect(content().string(not(containsString("pidió sumarse el 14 de octubre"))));
    mvc.perform(post("/admin-entidades-rechazada"))
        .andExpect(redirectedUrl("/admin-entidades-rechazada"));
    mvc.perform(get("/admin-entidades-registrar-error"))
        .andExpect(content().string(containsString("Falta la dirección: la usamos para")));
    mvc.perform(get("/admin-entidades-registrada"))
        .andExpect(content().string(containsString("necesita aprobación.")));
  }

  @Test
  void lasDonacionesDelAdminSeFiltranYSeMarcanVencidas() throws Exception {
    mvc.perform(get("/admin-donaciones"))
        .andExpect(model().attribute("seccion", "Donaciones"))
        .andExpect(content().string(containsString("7 en el depósito · 4 asignadas · 1 en camino")))
        .andExpect(content().string(containsString("Al 15 de octubre de 2026, 18:30.")))
        // R11-34: una bandeja; la ficha de partida es la de la primera y en el celular no se abre
        // sola
        .andExpect(content().string(containsString("data-elegido=\"partida\"")))
        .andExpect(
            content()
                .string(containsString("href=\"/admin-donaciones?donacion=leche#donacion-leche\"")))
        .andExpect(content().string(containsString("Buscar por lo que se donó")))
        .andExpect(content().string(containsString("Mostrando 1 a 5 de 14 donaciones.")))
        .andExpect(content().string(containsString("Ver las propuestas que la piden")));
    mvc.perform(get("/admin-donaciones?donacion=arroz"))
        .andExpect(content().string(containsString("aria-current=\"true\"")))
        .andExpect(content().string(not(containsString("data-elegido=\"partida\""))));
    // La pedida que no está en la página (los fideos están en la segunda) igual muestra su ficha
    mvc.perform(get("/admin-donaciones?donacion=fideos"))
        .andExpect(content().string(containsString("Cerrar la ficha")))
        .andExpect(content().string(containsString("Solo se puede marcar como vencida")));
    mvc.perform(get("/admin-donaciones?buscar=leche&categoria=alimentos"))
        .andExpect(content().string(containsString("Coincide 1 donación.")))
        .andExpect(content().string(containsString("Sacar los filtros")));
    mvc.perform(get("/admin-donaciones?donante=ana"))
        .andExpect(content().string(containsString("De Ana Ejemplo")))
        .andExpect(content().string(containsString("Quitar el filtro: De Ana Ejemplo")))
        .andExpect(content().string(not(containsString("Leche en polvo · 10 u"))));
    // R11-B04: el rango de fechas reemplaza al período fijo; en esos días no se registró ninguna
    mvc.perform(get("/admin-donaciones?desde=2026-10-16&hasta=2026-10-20"))
        .andExpect(
            content().string(containsString("Ninguna donación coincide con lo que buscaste.")));
    mvc.perform(get("/admin-donaciones?vence=pronto"))
        .andExpect(content().string(containsString("Vencen en los próximos 7 días")))
        .andExpect(content().string(containsString("Queso · 6 kg")))
        .andExpect(content().string(not(containsString("Arroz · 25 kg"))));
    mvc.perform(get("/admin-donaciones?estado=asignada"))
        .andExpect(content().string(containsString("Fideos · 20 kg")))
        .andExpect(content().string(not(containsString("Leche en polvo · 10 u"))));
    mvc.perform(get("/admin-donaciones?escenario=vacio"))
        .andExpect(content().string(containsString("Todavía no hay donaciones registradas.")));
    mvc.perform(get("/admin-donaciones?vence=pronto&escenario=vacio"))
        .andExpect(
            content()
                .string(
                    containsString("Ninguna donación del depósito vence en los próximos 7 días.")));
    mvc.perform(get("/admin-donaciones?escenario=error"))
        .andExpect(content().string(containsString("No pudimos cargar las donaciones.")));
    mvc.perform(get("/admin-donaciones-detalle"))
        .andExpect(model().attribute("seccion", "Donaciones"))
        .andExpect(content().string(containsString("¿Marcar Leche en polvo como vencida?")))
        .andExpect(content().string(containsString("popover")))
        .andExpect(
            content().string(containsString("action=\"/admin-donaciones-detalle-vencida\"")));
    mvc.perform(get("/admin-donaciones-detalle?donacion=fideos"))
        .andExpect(content().string(containsString("quedó asignada a Comedor Barrio Ejemplo")))
        .andExpect(content().string(containsString("Solo se puede marcar como vencida")))
        .andExpect(content().string(not(containsString("popovertarget=\"vencida\""))));
    mvc.perform(get("/admin-donaciones-detalle?escenario=cambio"))
        .andExpect(content().string(containsString("cambió mientras la mirabas")));
    mvc.perform(post("/admin-donaciones-detalle-vencida"))
        .andExpect(redirectedUrl("/admin-donaciones-detalle-vencida"));
    mvc.perform(get("/admin-donaciones-detalle-vencida"))
        .andExpect(content().string(containsString("La marcaste como vencida el 15 de octubre")))
        .andExpect(content().string(not(containsString("¿Marcar Leche en polvo como vencida?"))));
  }

  @Test
  void registrarDonacionEligeAlDonanteYSumaBienes() throws Exception {
    mvc.perform(get("/admin-donaciones-registrar"))
        .andExpect(content().string(containsString("Buscar por nombre o documento")))
        .andExpect(content().string(containsString("enctype=\"multipart/form-data\"")))
        .andExpect(content().string(containsString("formnovalidate")))
        .andExpect(content().string(not(containsString("Bien 2"))));
    mvc.perform(get("/admin-donaciones-registrar?donante=ana-ejemplo&bienes=2"))
        .andExpect(content().string(containsString("<b>Ana Ejemplo</b>")))
        .andExpect(content().string(containsString("Bien 2")))
        .andExpect(content().string(containsString("value=\"ana-ejemplo\"")));
    mvc.perform(get("/admin-donaciones-registrar?buscar=ejemplo"))
        .andExpect(content().string(containsString("2 donantes coinciden con la búsqueda.")));
    mvc.perform(get("/admin-donaciones-registrar-error"))
        .andExpect(content().string(containsString("Revisá 2 datos para seguir.")))
        .andExpect(content().string(containsString("aria-invalid=\"true\"")));
    mvc.perform(get("/admin-donaciones-registrar-error?escenario=inactivo"))
        .andExpect(
            content().string(containsString("Pedro Ejemplo está dado de baja como donante")));
    mvc.perform(post("/admin-donaciones-registrada"))
        .andExpect(redirectedUrl("/admin-donaciones-registrada"));
    mvc.perform(get("/admin-donaciones-registrada"))
        .andExpect(content().string(containsString("Caja de alimentos, 3 bienes.")));
    mvc.perform(get("/admin-donaciones-detalle?donacion=caja"))
        .andExpect(content().string(containsString("Caja de alimentos")))
        .andExpect(content().string(containsString("Los 3 bienes de la donación")))
        .andExpect(content().string(containsString("10 litros")));
  }

  @Test
  void lasPropuestasSeApruebanODescartan() throws Exception {
    mvc.perform(get("/admin-asignaciones"))
        .andExpect(model().attribute("seccion", "Asignar donaciones"))
        .andExpect(content().string(containsString("Propuestas sin problemas · 4")))
        .andExpect(content().string(containsString("Cubre 10 de 12 unidades")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "aria-label=\"Aprobar: propuesta para Comedor Barrio Ejemplo\"")))
        .andExpect(content().string(containsString("Hay 4 propuestas en conflicto.")))
        .andExpect(content().string(containsString("Ver todas las decididas")));
    mvc.perform(get("/admin-asignaciones?escenario=vacio"))
        .andExpect(content().string(containsString("No hay propuestas que esperen tu decisión.")));
    mvc.perform(get("/admin-asignaciones?escenario=corriendo"))
        .andExpect(content().string(containsString("Corriendo la asignación…")));
    mvc.perform(get("/admin-asignaciones?algoritmo=las-dos"))
        .andExpect(content().string(containsString("Propuestas sin problemas · 1")))
        .andExpect(
            content()
                .string(
                    containsString("Las donaciones son las que eligió Quienes menos recibieron.")));
    mvc.perform(get("/admin-asignaciones?pestana=conflictos"))
        .andExpect(content().string(containsString("con 1 propuesta más: alcanza para una.")))
        .andExpect(
            content().string(not(containsString("Aprobar: propuesta para Hogar Ejemplo Centro"))));
    mvc.perform(get("/admin-asignaciones?pestana=destino"))
        .andExpect(content().string(containsString("¿Adónde va")))
        .andExpect(content().string(containsString("2 propuestas · no alcanza")));
    mvc.perform(get("/admin-asignaciones?pestana=destino&donacion=otra"))
        .andExpect(
            content()
                .string(containsString("Esa donación ya no está en ninguna propuesta pendiente.")));
    mvc.perform(get("/admin-asignaciones?vista=decididas"))
        .andExpect(content().string(containsString("Decididas · 7")))
        .andExpect(content().string(containsString("Buscar por organización o donación")))
        .andExpect(content().string(containsString("Aprobó Leo Gómez el 9 de noviembre de 2026")))
        .andExpect(content().string(containsString("Sin motivo anotado.")))
        .andExpect(content().string(containsString("Mostrando 1 a 7 de 7 propuestas decididas.")));
    // R11-35: la búsqueda y los filtros de Todas las decididas
    mvc.perform(get("/admin-asignaciones?vista=decididas&estado=descartadas"))
        .andExpect(content().string(containsString("Coinciden 2 propuestas decididas.")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "Motivo: «El comedor ya consiguió galletitas de otra donación.»")))
        .andExpect(content().string(containsString("Sacar los filtros")));
    // R11-B04: el rango de fechas reemplaza al período fijo "Decidida"
    mvc.perform(get("/admin-asignaciones?vista=decididas&desde=2026-11-06&hasta=2026-11-12"))
        .andExpect(content().string(containsString("Hay 2 decididas de antes de que guardáramos")));
    mvc.perform(get("/admin-asignaciones?vista=decididas&buscar=nada-parecido"))
        .andExpect(
            content()
                .string(
                    containsString("Ninguna propuesta decidida coincide con lo que buscaste.")));
    mvc.perform(get("/admin-asignaciones-aprobada"))
        .andExpect(content().string(containsString("Aprobaste la propuesta para Comedor Barrio")))
        .andExpect(content().string(containsString("Propuestas sin problemas · 3")));
    mvc.perform(get("/admin-asignaciones-descartada"))
        .andExpect(content().string(containsString("Descartaste la propuesta para Merendero")));
    mvc.perform(get("/admin-asignaciones-error"))
        .andExpect(content().string(containsString("ya no está activa")))
        .andExpect(
            content().string(not(containsString("Aprobar: propuesta para Merendero Ejemplo Sur"))));
    mvc.perform(get("/admin-asignaciones-error?escenario=propia"))
        .andExpect(content().string(containsString("de la misma organización que la")));
    mvc.perform(get("/admin-asignaciones-error?escenario=cambio"))
        .andExpect(content().string(containsString("Volvimos a cargar la lista.")));
    mvc.perform(post("/admin-asignaciones-corrida"))
        .andExpect(redirectedUrl("/admin-asignaciones-corrida"));
    mvc.perform(get("/admin-asignaciones-corrida?escenario=nuevas"))
        .andExpect(content().string(containsString("generó 2 propuestas nuevas.")))
        .andExpect(content().string(containsString("Propuestas sin problemas · 6")));
  }

  @Test
  void losCamionesSeAgreganYSeDeshabilitanConSuMotivo() throws Exception {
    mvc.perform(get("/admin-camiones"))
        .andExpect(model().attribute("seccion", "Camiones"))
        .andExpect(
            content()
                .string(containsString("5 camiones: 1 en ruta · 3 disponibles · 1 deshabilitado")))
        .andExpect(content().string(containsString("Al 15 de octubre de 2026, 18:30.")))
        .andExpect(content().string(containsString("En el taller desde el 12 de octubre.")))
        .andExpect(content().string(containsString("action=\"/admin-camiones-agregado\"")))
        .andExpect(content().string(not(containsString("Deshabilitar: camión AB 123 CD"))));
    // La ficha de un camión disponible trae su "Deshabilitar" con el diálogo del motivo.
    mvc.perform(get("/admin-camiones?camion=ac456ef"))
        .andExpect(content().string(containsString("¿Deshabilitar el camión AC 456 EF?")))
        .andExpect(
            content().string(containsString("aria-label=\"Deshabilitar: camión AC 456 EF\"")));
    mvc.perform(get("/admin-camiones?escenario=vacio"))
        .andExpect(content().string(containsString("Todavía no hay camiones.")));
    mvc.perform(get("/admin-camiones?escenario=lento"))
        .andExpect(content().string(containsString("Cargando los camiones…")));
    mvc.perform(get("/admin-camiones?escenario=error"))
        .andExpect(content().string(containsString("No pudimos cargar los camiones.")));
    mvc.perform(post("/admin-camiones-agregado"))
        .andExpect(redirectedUrl("/admin-camiones-agregado"));
    mvc.perform(get("/admin-camiones-agregado"))
        .andExpect(content().string(containsString("Agregaste el camión AF 345 KL.")))
        .andExpect(content().string(containsString("6 camiones: 1 en ruta · 4 disponibles")));
    mvc.perform(get("/admin-camiones-agregar-error"))
        .andExpect(content().string(containsString("Revisá 2 datos para seguir.")))
        .andExpect(content().string(containsString("aria-invalid=\"true\"")));
    mvc.perform(get("/admin-camiones-deshabilitado"))
        .andExpect(content().string(containsString("Deshabilitaste el camión AC 456 EF.")))
        .andExpect(content().string(containsString("Service de frenos. Desde hoy, 18:35.")))
        .andExpect(content().string(containsString("Al 15 de octubre de 2026, 18:35.")))
        .andExpect(content().string(containsString("Volver a habilitar: camión AC 456 EF")));
    mvc.perform(get("/admin-camiones-habilitado"))
        .andExpect(content().string(containsString("Volviste a habilitar el camión AA 999 ZZ.")))
        .andExpect(content().string(containsString("Deshabilitar: camión AA 999 ZZ")));
  }

  @Test
  void elRankingDiceSuCriterioYSeFiltraPorMes() throws Exception {
    mvc.perform(get("/admin-ranking"))
        .andExpect(model().attribute("seccion", "Ranking"))
        .andExpect(content().string(containsString("Septiembre de 2026")))
        .andExpect(content().string(containsString("Ranking de donantes de septiembre de 2026")))
        .andExpect(content().string(not(containsString("Volver al último mes"))))
        .andExpect(content().string(containsString("misiones que cada donante completó en el mes")))
        .andExpect(
            content().string(containsString("Calculado el 30 de septiembre de 2026, 23:59.")))
        .andExpect(content().string(containsString("hx-boost=\"true\"")))
        .andExpect(content().string(containsString("<caption>")));
    mvc.perform(get("/admin-ranking?mes=2026-08"))
        .andExpect(content().string(containsString("Ranking de donantes de agosto de 2026")))
        .andExpect(content().string(containsString("value=\"2026-08\" checked")))
        .andExpect(content().string(containsString("Volver al último mes")))
        .andExpect(content().string(not(containsString("Sacar los filtros"))));
    mvc.perform(get("/admin-ranking?mes=2026-09"))
        .andExpect(content().string(containsString("Ranking de donantes de septiembre de 2026")))
        .andExpect(content().string(not(containsString("Volver al último mes"))));
    mvc.perform(get("/admin-ranking?escenario=vacio"))
        .andExpect(content().string(containsString("Todavía no hay ningún ranking")));
    mvc.perform(get("/admin-ranking?escenario=error"))
        .andExpect(content().string(containsString("No pudimos cargar el ranking.")));
    mvc.perform(get("/admin-ranking?respuesta=404")).andExpect(status().isNotFound());
  }

  /** Los formularios de la persona que dona vuelven a su pantalla, como los de la empresa. */
  @Test
  void losFormulariosDeLaPersonaQueDonaRedirigen() throws Exception {
    mvc.perform(post("/donante-perfil-guardado").sessionAttr("cuenta", "persona"))
        .andExpect(redirectedUrl("/donante-perfil-guardado"));
    mvc.perform(
            post("/donante-notificaciones-leidas")
                .sessionAttr("cuenta", "persona")
                .param("marcar", "n1"))
        .andExpect(redirectedUrl("/donante-notificaciones-leidas?marcadas=n1"));
  }

  /** Cada pantalla propia de la persona que dona se arma sin errores (usuario, 2026-11-10). */
  @Test
  void lasPantallasDeLaPersonaQueDonaSeArman() throws Exception {
    for (String pantalla :
        List.of(
            "donante-inicio",
            "donante-donaciones",
            "donante-entidades",
            "donante-seguimiento",
            "donante-notificaciones")) {
      mvc.perform(get("/" + pantalla + "?cuenta=persona")).andExpect(status().isOk());
    }
    mvc.perform(get("/donante-entidades?cuenta=persona"))
        .andExpect(content().string(containsString("class=\"progreso\"")));
  }

  private static final Pattern ENLACE = Pattern.compile("href=\"([^\"]*)\"");

  /** Lo recibido de una necesidad: el número en negrita y, al lado, de cuánto es la meta. */
  private static ResultMatcher recibidos(String valor, String meta) {
    return content()
        .string(
            matchesPattern(
                Pattern.compile(
                    "(?s).*<b>" + valor + "</b>\\s*<span>" + Pattern.quote(meta) + "</span>.*")));
  }

  @Test
  void laPersonaQueDonaVeSusMisionesYLaEmpresaNo() throws Exception {
    mvc.perform(get("/donante-misiones?cuenta=persona"))
        .andExpect(status().isOk())
        .andExpect(view().name("donante-misiones"))
        .andExpect(model().attribute("persona", true))
        .andExpect(content().string(containsString("Martina Ruiz")))
        .andExpect(content().string(containsString("Persona donante")))
        .andExpect(content().string(containsString("Tus misiones e insignias")))
        .andExpect(content().string(containsString("Sos donante Sostenedor desde el")))
        .andExpect(
            content().string(containsString("href=\"/donante-misiones\" aria-current=\"page\"")))
        .andExpect(content().string(containsString("aria-label=\"Gran Aporte: 12 de 50\"")))
        .andExpect(content().string(containsString("Completa el 3 de agosto de 2026")))
        .andExpect(
            content().string(containsString("Empieza a contar cuando completes Gran Aporte.")))
        .andExpect(
            content()
                .string(containsString("Sus misiones se ven cuando completes las de Sostenedor.")))
        .andExpect(content().string(containsString("aria-label=\"Tus insignias\"")))
        .andExpect(content().string(containsString("(2 de 6)")))
        .andExpect(
            content()
                .string(containsString("<details class=\"plegable\" data-categoria=\"completa\">")))
        .andExpect(content().string(containsString("Colaborador · completa")))
        .andExpect(content().string(containsString("Ver las misiones")))
        .andExpect(content().string(containsString("Llevás 12 de 50.")))
        .andExpect(content().string(containsString("aria-label=\"Gran Aporte, en curso\"")))
        .andExpect(content().string(containsString("data-papel=\"hueco\"")))
        .andExpect(content().string(not(containsString("Notificaciones, 4 sin leer"))));
    mvc.perform(get("/donante-misiones?escenario=vacio").sessionAttr("cuenta", "persona"))
        .andExpect(content().string(containsString("Todavía no tenés insignias")))
        .andExpect(content().string(containsString("Llevás 0 de 3.")))
        .andExpect(content().string(containsString("(0 de 6)")));
    mvc.perform(get("/donante-misiones?escenario=lento").sessionAttr("cuenta", "persona"))
        .andExpect(content().string(containsString("role=\"status\">Cargando tus misiones")))
        .andExpect(content().string(containsString("aria-busy=\"true\"")));
    mvc.perform(get("/donante-misiones?escenario=error").sessionAttr("cuenta", "persona"))
        .andExpect(content().string(containsString("role=\"alert\"")))
        .andExpect(content().string(containsString("No pudimos cargar tus misiones.")))
        .andExpect(content().string(containsString("a1b2c3d4e5f6")));
    mvc.perform(get("/donante-misiones"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("persona", false))
        .andExpect(content().string(containsString("es para quienes donan como persona")))
        .andExpect(content().string(not(containsString("Gran Aporte"))))
        .andExpect(content().string(not(containsString("href=\"/donante-misiones\""))));
  }

  @Test
  void conLaPersonaLasOtrasDelDonanteMuestranSuVersionOElAviso() throws Exception {
    mvc.perform(get("/donante-donaciones").sessionAttr("cuenta", "persona"))
        .andExpect(status().isOk())
        .andExpect(view().name("persona/donante-donaciones"))
        .andExpect(model().attribute("seccion", "Donaciones"))
        .andExpect(content().string(containsString("Martina Ruiz")));
    mvc.perform(get("/donante-notificaciones").sessionAttr("cuenta", "persona"))
        .andExpect(view().name("persona/donante-notificaciones"))
        .andExpect(content().string(containsString("<h1>Novedades</h1>")));
    mvc.perform(get("/donante-perfil").sessionAttr("cuenta", "persona"))
        .andExpect(view().name("persona/donante-perfil"))
        .andExpect(model().attribute("seccion", "Mi perfil"));
    // Una pantalla del donante sin versión de la persona sigue mostrando el aviso.
    mvc.perform(get("/donante-donaciones-nueva").sessionAttr("cuenta", "persona"))
        .andExpect(status().isOk())
        .andExpect(view().name("aviso-persona"))
        .andExpect(model().attribute("seccion", "Donaciones"))
        .andExpect(content().string(containsString("la maqueta solo muestra Misiones e insignias")))
        .andExpect(content().string(containsString("href=\"/donante-inicio?cuenta=empresa\"")));
    mvc.perform(get("/donante-inicio?cuenta=empresa").sessionAttr("cuenta", "persona"))
        .andExpect(view().name("donante-inicio"))
        .andExpect(content().string(containsString("Distribuidora Ejemplo SA")));
    mvc.perform(get("/entidad-inicio").sessionAttr("cuenta", "persona"))
        .andExpect(view().name("entidad-inicio"));
    mvc.perform(get("/publico-ingresar"))
        .andExpect(content().string(containsString("href=\"/donante-misiones?cuenta=persona\"")))
        .andExpect(content().string(containsString("Martina Ruiz, persona donante")));
  }

  // DA-02: Donaciones recibidas y Seguir entregas son una sola pantalla, Entregas (arquetipo A10).
  @Test
  void laEntidadVeSusEntregasEnUnaSolaPantalla() throws Exception {
    String entregas =
        mvc.perform(get("/entidad-seguimiento"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("seccion", "Entregas"))
            .andExpect(
                content()
                    .string(containsString("href=\"/entidad-seguimiento\" aria-current=\"page\"")))
            .andExpect(content().string(containsString("<h1>Entregas</h1>")))
            .andExpect(content().string(containsString("role=\"search\"")))
            .andExpect(content().string(containsString("Para confirmar")))
            .andExpect(content().string(containsString("data-papel=\"protagonista\"")))
            .andExpect(content().string(containsString("Espera tu confirmación")))
            .andExpect(content().string(containsString("entrega=lentejas")))
            .andExpect(content().string(containsString("entrega=ropa")))
            .andExpect(content().string(containsString("Por llegar")))
            .andExpect(content().string(containsString("Última noticia: 14 de octubre, 16:40.")))
            .andExpect(content().string(containsString("actualizada hoy a las 18:25")))
            .andExpect(
                content()
                    .string(containsString("href=\"/entidad-entrega-problema?entrega=aceite\"")))
            .andExpect(content().string(containsString("Todavía sin fecha de nueva salida.")))
            .andExpect(content().string(containsString("Confirmadas en octubre")))
            .andExpect(
                content().string(containsString("Recibió: Comedor Barrio Ejemplo, el 10 de")))
            // A10 (D3): cada confirmada es su etiqueta, con la foto y el vínculo a lo asentado
            .andExpect(
                content()
                    .string(containsString("class=\"etiqueta-entrega etiqueta-entrega--costado\"")))
            .andExpect(content().string(containsString("/img/ilustraciones/escena-puerta.svg")))
            .andExpect(content().string(containsString("Foto de ejemplo")))
            .andExpect(
                content()
                    .string(containsString("href=\"/entidad-seguimiento-sellada?entrega=r10\"")))
            .andExpect(content().string(not(containsString("entidad-recibidas"))))
            .andReturn()
            .getResponse()
            .getContentAsString();
    // Las tarjetas van en un orden fijo: por confirmar, con problema, por salir y confirmadas.
    int confirmar = entregas.indexOf("id=\"confirmar-titulo\"");
    int aceite = entregas.indexOf("id=\"entrega-aceite\"");
    int leche = entregas.indexOf("id=\"entrega-leche\"");
    int confirmadas = entregas.indexOf("id=\"confirmadas-titulo\"");
    assertTrue(confirmar > 0 && confirmar < aceite, "por confirmar, antes que con problema");
    assertTrue(aceite < leche, "con problema, antes que por salir");
    assertTrue(leche < confirmadas, "por salir, antes que las confirmadas");
    mvc.perform(get("/entidad-seguimiento?escenario=sin-puerta"))
        .andExpect(content().string(containsString("No tenés entregas para")))
        .andExpect(content().string(not(containsString("Para confirmar"))))
        .andExpect(content().string(not(containsString("data-papel=\"protagonista\""))));
    mvc.perform(get("/entidad-seguimiento?estado=recibidas"))
        .andExpect(content().string(containsString("Mostrando: Confirmadas (6).")))
        .andExpect(content().string(not(containsString("Por llegar"))));
    mvc.perform(get("/entidad-seguimiento?escenario=sin-resultados"))
        .andExpect(content().string(containsString("No hay entregas en ese estado.")));
    mvc.perform(get("/entidad-seguimiento?escenario=vacio"))
        .andExpect(
            content()
                .string(containsString("Todavía no se asignaron donaciones a tu organización.")))
        .andExpect(content().string(not(containsString("Por llegar"))));
    mvc.perform(get("/entidad-seguimiento?escenario=lento"))
        .andExpect(content().string(containsString("role=\"status\">Cargando tus entregas")));
    mvc.perform(get("/entidad-seguimiento?escenario=error"))
        .andExpect(content().string(containsString("No pudimos cargar tus entregas.")))
        .andExpect(content().string(containsString("role=\"alert\"")));
  }

  // DA-02: la ruta vieja de Donaciones recibidas lleva a Entregas con la misma consulta.
  @Test
  void donacionesRecibidasRedirigeAEntregasConLaMismaConsulta() throws Exception {
    mvc.perform(get("/entidad-recibidas")).andExpect(redirectedUrl("/entidad-seguimiento"));
    mvc.perform(get("/entidad-recibidas?estado=en-camino&categoria=ropa"))
        .andExpect(redirectedUrl("/entidad-seguimiento?estado=en-camino&categoria=ropa"));
  }

  // ORG-08: la entrega que no pudo llegar tiene su página, dentro de Entregas (arquetipo A11).
  @Test
  void laEntregaConProblemaDiceQuePasoYQueSigue() throws Exception {
    mvc.perform(get("/entidad-entrega-problema?entrega=aceite"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("seccion", "Entregas"))
        .andExpect(
            content().string(containsString("href=\"/entidad-seguimiento\" aria-current=\"page\"")))
        .andExpect(content().string(containsString("<h1>Aceite de girasol, 10 litros</h1>")))
        .andExpect(content().string(containsString("El camión no pudo llegar el 14 de octubre.")))
        .andExpect(content().string(containsString("<dd>Todavía sin fecha de nueva salida</dd>")))
        .andExpect(content().string(containsString("<dd>14 de octubre, 16:40.</dd>")))
        .andExpect(content().string(containsString("Tu necesidad de aceite de girasol")))
        // A11: el avance de la necesidad, con la cuenta de Necesidades (HG-04)
        .andExpect(content().string(containsString("Una sola vez · Hasta el 20 de octubre")))
        .andExpect(content().string(containsString("aria-label=\"Llegaron 0 de 20 litros\"")))
        .andExpect(
            content()
                .string(containsString("Llegaron 0 de 20 litros. Esta entrega trae 10 litros.")))
        .andExpect(content().string(containsString("Al 15 de octubre de 2026.")))
        .andExpect(content().string(containsString("Tu foto de la entrega")))
        .andExpect(content().string(containsString("Volver a")))
        .andExpect(content().string(not(containsString("17 de octubre"))));
    mvc.perform(get("/entidad-entrega-problema?entrega=pan"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("No encontramos esa entrega con problema.")));
  }

  // DA-02 y DA-05: el menú de la organización es Inicio, Necesidades, Entregas y Ayuda; Ayuda es
  // la página Para organizaciones.
  @Test
  void elMenuDeLaOrganizacionEsInicioNecesidadesEntregasYAyuda() throws Exception {
    String inicio =
        mvc.perform(get("/entidad-inicio"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String menu = inicio.substring(inicio.indexOf("id=\"menu\""));
    menu = menu.substring(0, menu.indexOf("</nav>"));
    List<String> secciones =
        Pattern.compile("<span>([^<]+)</span>")
            .matcher(menu)
            .results()
            .map(m -> m.group(1))
            .toList();
    assertEquals(List.of("Inicio", "Necesidades", "Entregas", "Ayuda"), secciones);
    assertTrue(menu.contains("href=\"/entidad-seguimiento\""), menu);
    assertTrue(menu.contains("href=\"/publico-para-entidades\""), menu);
    assertTrue(menu.contains("#nav-ayuda"), menu);
    assertFalse(menu.contains("entidad-recibidas"), menu);
  }

  @Test
  void elPerfilDeLaEntidadGuardaSusDatosYSusAvisos() throws Exception {
    mvc.perform(get("/entidad-perfil"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("seccion", "Mi perfil"))
        .andExpect(content().string(containsString("aria-label=\"Secciones de tu perfil\"")))
        .andExpect(content().string(containsString("Datos de la organización")))
        .andExpect(content().string(containsString("Calle Ejemplo 450, Localidad Ejemplo")))
        .andExpect(content().string(containsString("No se cambia desde acá")))
        .andExpect(content().string(not(containsString("Cómo te ven"))))
        .andExpect(content().string(containsString("Aceptaste la versión 1")));
    mvc.perform(post("/entidad-perfil-guardado").param("seccion", "avisos"))
        .andExpect(redirectedUrl("/entidad-perfil-guardado?seccion=avisos"));
    mvc.perform(get("/entidad-perfil-guardado?seccion=datos"))
        .andExpect(content().string(containsString("Guardamos los datos de tu organización.")));
    mvc.perform(get("/entidad-perfil-guardado?seccion=avisos"))
        .andExpect(content().string(containsString("Guardamos cómo te avisamos.")));
    mvc.perform(get("/entidad-perfil?escenario=lento"))
        .andExpect(content().string(containsString("Guardando…")));
    mvc.perform(get("/entidad-perfil?escenario=error"))
        .andExpect(content().string(containsString("No pudimos guardar los cambios.")));
    mvc.perform(get("/entidad-perfil?escenario=sin-conexion"))
        .andExpect(content().string(containsString("No sabemos si se guardó.")));
  }

  /**
   * DA-05 (PUB1-07): Nosotros, Para donantes y Para organizaciones siguen en sus rutas y se llaman
   * «Cómo funciona», «Cómo donar» y «Ayuda». Derivan de A01: el título sobre la trama, la raíz
   * numerada de 5 pasos con el recorrido y una sola acción llena por página.
   */
  @Test
  void lasPaginasParaConocerTienenSuNombreNuevoYLaComposicionDeLaPortada() throws Exception {
    List<String[]> paginas =
        List.of(
            new String[] {"/publico-nosotros", "Cómo funciona"},
            new String[] {"/publico-para-donantes", "Cómo donar"},
            new String[] {"/publico-para-entidades", "Ayuda"});
    for (String[] pagina : paginas) {
      String html =
          mvc.perform(get(pagina[0]))
              .andExpect(status().isOk())
              .andExpect(view().name(pagina[0].substring(1)))
              .andReturn()
              .getResponse()
              .getContentAsString(StandardCharsets.UTF_8);
      String principal = html.substring(html.indexOf("<main"), html.indexOf("</main>"));
      assertTrue(html.contains("<title>" + pagina[1] + " - DonaTrack</title>"), pagina[0]);
      assertTrue(principal.contains("<p class=\"huerta-rotulo\">" + pagina[1] + "</p>"), pagina[0]);
      assertTrue(principal.matches("(?s).*<div class=\"trama\">\\s*<h1>.*"), pagina[0]);
      assertTrue(
          principal.matches(
              "(?s).*<ol class=\"huerta-raiz-pasos\" role=\"list\"\\s+data-forma=\"numerada tendida\".*"),
          pagina[0]);
      assertEquals(5, cuantasVeces(principal, "class=\"huerta-raiz-nudo\""), pagina[0]);
      assertEquals(1, cuantasVeces(principal, "data-tono=\"principal\""), pagina[0]);
    }
  }

  /**
   * DA-23 (PUB1-08): el pie nombra las tres páginas con su nombre nuevo, con las rutas y el orden.
   */
  @Test
  void elPieNombraLasPaginasParaConocerConSuNombreNuevo() throws Exception {
    for (String pantalla : List.of("/publico-legales", "/publico-nosotros", "/donante-inicio")) {
      String html =
          mvc.perform(get(pantalla))
              .andReturn()
              .getResponse()
              .getContentAsString(StandardCharsets.UTF_8);
      String pie = html.substring(html.indexOf("<footer class=\"raices-pie\""));
      List<String> enlaces = new ArrayList<>();
      Matcher enlace = Pattern.compile("<a href=\"([^\"]+)\"[^>]*>([^<]+)</a>").matcher(pie);
      while (enlace.find()) {
        enlaces.add(enlace.group(1) + " " + enlace.group(2));
      }
      assertEquals(
          List.of(
              "/publico-nosotros Cómo funciona",
              "/publico-para-donantes Cómo donar",
              "/publico-para-entidades Ayuda",
              "/publico-legales Términos y privacidad",
              "/publico-contacto Contacto"),
          enlaces,
          pantalla);
    }
  }

  private static int cuantasVeces(String texto, String buscado) {
    int n = 0;
    for (int i = texto.indexOf(buscado); i >= 0; i = texto.indexOf(buscado, i + 1)) {
      n++;
    }
    return n;
  }

  @Test
  void estanDibujadasLasPantallasDeLosTresRecorridos() throws Exception {
    for (String pantalla : DIBUJADAS) {
      mvc.perform(get("/" + pantalla)).andExpect(status().isOk()).andExpect(view().name(pantalla));
    }
  }

  /**
   * Desde las portadas de cada rol sigue, en anchura, cada enlace que queda dentro del sitio: todos
   * responden 200. El enlace roto se arregla en la plantilla, no acá.
   */
  @Test
  void ningunEnlaceDelSitioEstaRoto() throws Exception {
    Deque<String> pendientes = new ArrayDeque<>();
    Set<String> vistos = new LinkedHashSet<>();
    for (String inicio : List.of("/", "/donante-inicio", "/entidad-inicio", "/admin-inicio")) {
      pendientes.add(inicio);
      vistos.add(inicio);
    }
    while (!pendientes.isEmpty()) {
      String direccion = pendientes.poll();
      MvcResult resultado =
          mvc.perform(get(URI.create(direccion))).andExpect(status().isOk()).andReturn();
      assertNotEquals(
          "sin-dibujar",
          resultado.getModelAndView() == null ? null : resultado.getModelAndView().getViewName(),
          direccion + " lleva a la página sin dibujar (F7 del roadmap de vistas)");
      Matcher enlace = ENLACE.matcher(resultado.getResponse().getContentAsString());
      while (enlace.find()) {
        String destino = enlace.group(1).replace("&amp;", "&");
        int ancla = destino.indexOf('#');
        if (ancla >= 0) {
          destino = destino.substring(0, ancla);
        }
        boolean pantalla =
            destino.startsWith("/")
                && !destino.startsWith("//")
                && !destino.startsWith("/css/")
                && !destino.startsWith("/img/")
                && !destino.startsWith("/js/")
                && !destino.startsWith("/fuentes/");
        if (pantalla && vistos.add(destino)) {
          pendientes.add(destino);
        }
      }
    }
    assertTrue(vistos.size() > 40, "el recorrido llegó a pocas pantallas");
  }
}
