package grupo5.cliente.maqueta;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import jakarta.servlet.RequestDispatcher;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.autoconfigure.error.DefaultErrorViewResolver;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** La pantalla de prueba está en src/test/resources/templates/. */
@WebMvcTest(SitioController.class)
@Import({MaquetaConfig.class, SitioControllerTest.PaginasDeError.class})
@ActiveProfiles("maqueta")
class SitioControllerTest {

  /**
   * En @WebMvcTest no hay DispatcherServlet de verdad, y Spring Boot no registra el resolvedor que
   * elige templates/error/&lt;código&gt;: se registra acá, igual que en la aplicación.
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
        .andExpect(model().attribute("seccion", "Seguir entregas"))
        .andExpect(content().string(containsString("todavía no está dibujada")))
        .andExpect(content().string(containsString("href=\"/entidad-inicio\"")));
  }

  @Test
  void loQueNoEstaNiEnElMenuNiDibujadoNoExiste() throws Exception {
    mvc.perform(get("/donante-tesoro")).andExpect(status().isNotFound());
    mvc.perform(get("/..%2Ftemplates%2Fpie")).andExpect(status().isNotFound());
    mvc.perform(get("/sin-dibujar")).andExpect(status().isNotFound());
    mvc.perform(get("/datos-donante")).andExpect(status().isNotFound());
    mvc.perform(get("/perfil")).andExpect(status().isNotFound());
  }

  @Test
  void laAdministracionNoTieneMiPerfil() throws Exception {
    mvc.perform(get("/admin-inicio"))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString("/admin-perfil"))));
    mvc.perform(get("/admin-perfil"))
        .andExpect(status().isOk())
        .andExpect(view().name("sin-dibujar"))
        .andExpect(model().attribute("seccion", "Mi perfil"));
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
        .andExpect(model().attribute("seccion", "Mis donaciones"))
        .andExpect(
            content().string(containsString("href=\"/donante-donaciones\" aria-current=\"page\"")));
  }

  @Test
  void loQueNoEstaDibujadoMuestraElAviso() throws Exception {
    mvc.perform(get("/donante-entidades-detalle?entidad=comedor"))
        .andExpect(status().isOk())
        .andExpect(view().name("sin-dibujar"))
        .andExpect(model().attribute("seccion", "Entidades"))
        .andExpect(content().string(containsString("Empresa donante")))
        .andExpect(content().string(not(containsString("entidad=comedor\""))));
    mvc.perform(get("/donante-incentivos")).andExpect(status().isNotFound());
    mvc.perform(get("/donante-tesoro")).andExpect(status().isNotFound());
    mvc.perform(get("/sin-dibujar")).andExpect(status().isNotFound());
    mvc.perform(get("/publico-perfil")).andExpect(status().isNotFound());
    mvc.perform(post("/donante-tesoro")).andExpect(status().isNotFound());
  }

  @Test
  void elSeguimientoMuestraCadaEstado() throws Exception {
    mvc.perform(get("/donante-seguimiento"))
        .andExpect(status().isOk())
        .andExpect(view().name("donante-seguimiento"))
        .andExpect(model().attribute("seccion", "Seguimiento"))
        .andExpect(content().string(containsString("3 entregas en curso")))
        .andExpect(content().string(containsString("aria-current=\"step\"")))
        .andExpect(content().string(containsString("aria-labelledby=\"lista-titulo\"")));
    mvc.perform(get("/donante-seguimiento?escenario=vacio"))
        .andExpect(
            content().string(containsString("Ningún camión está en camino con tus donaciones")));
    mvc.perform(get("/donante-seguimiento?escenario=error"))
        .andExpect(content().string(containsString("role=\"alert\"")))
        .andExpect(content().string(containsString("Volver a intentar")));
    mvc.perform(get("/donante-seguimiento?escenario=lento"))
        .andExpect(content().string(containsString("aria-busy=\"true\"")));
  }

  @Test
  void elDonanteRecorreSuInicioSusDonacionesEntidadesYNotificaciones() throws Exception {
    mvc.perform(get("/donante-inicio"))
        .andExpect(status().isOk())
        .andExpect(view().name("donante-inicio"))
        .andExpect(content().string(containsString("Primera entrega confirmada: fideos secos")))
        .andExpect(content().string(containsString("aria-label=\"Notificaciones, 4 sin leer\"")));
    mvc.perform(get("/donante-inicio?escenario=lento"))
        .andExpect(content().string(containsString("aria-busy=\"true\"")));
    mvc.perform(get("/donante-donaciones?estado=pendiente&categoria=alimentos"))
        .andExpect(content().string(containsString("2 donaciones con esos filtros.")));
    mvc.perform(get("/donante-donaciones?estado=otro"))
        .andExpect(content().string(containsString("7 donaciones, de la más reciente")));
    mvc.perform(get("/donante-donaciones-detalle?donacion=ropa"))
        .andExpect(model().attribute("seccion", "Mis donaciones"))
        .andExpect(content().string(containsString("confirmó la entrega el 9 de octubre")));
    mvc.perform(get("/donante-donaciones-detalle?donacion=pan"))
        .andExpect(content().string(containsString("No encontramos esa donación.")));
    mvc.perform(get("/donante-entidades?nombre=zzz"))
        .andExpect(content().string(containsString("Ninguna entidad activa se llama así.")));
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
        .andExpect(model().attribute("seccion", "Seguir entregas"))
        .andExpect(
            content().string(containsString("Lentejas secas, 15 kg. Llegó hoy a las 18:20.")))
        .andExpect(content().string(containsString("enctype=\"multipart/form-data\"")))
        .andExpect(
            content().string(containsString("action=\"/entidad-seguimiento-sellada-lentejas\"")))
        .andExpect(content().string(containsString("aria-current=\"step\"")))
        .andExpect(content().string(containsString("name=\"fotos\"")));
    mvc.perform(get("/entidad-seguimiento-confirmar?entrega=ropa"))
        .andExpect(content().string(containsString("Donó</dt>")))
        .andExpect(content().string(containsString("Donante Sostenedor")));
    mvc.perform(get("/entidad-seguimiento-confirmar?entrega=pan"))
        .andExpect(content().string(containsString("No encontramos esa entrega")));
    mvc.perform(get("/entidad-seguimiento-confirmar?escenario=confirmada"))
        .andExpect(content().string(containsString("Ya confirmaste esta entrega")))
        .andExpect(content().string(containsString("Esta entrega ya está <em>confirmada</em>")))
        .andExpect(content().string(not(containsString("Van 10 kg recibidos."))));
    mvc.perform(get("/entidad-seguimiento-confirmar?escenario=sin-conexion"))
        .andExpect(content().string(containsString("No sabemos si la confirmación llegó.")));
    mvc.perform(get("/entidad-seguimiento-confirmar-error"))
        .andExpect(content().string(containsString("Revisá dos cosas antes de confirmar")))
        .andExpect(content().string(containsString("aria-invalid=\"true\"")));
  }

  @Test
  void confirmarLlevaALaRaizCompletaYALaQueSigue() throws Exception {
    mvc.perform(post("/entidad-seguimiento-sellada-lentejas"))
        .andExpect(redirectedUrl("/entidad-seguimiento-sellada-lentejas"));
    mvc.perform(get("/entidad-seguimiento-sellada-lentejas"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("confirmada--recien")))
        .andExpect(content().string(containsString("Te queda una en la puerta")))
        .andExpect(content().string(containsString("entidad-seguimiento-confirmar?entrega=ropa")));
    mvc.perform(get("/entidad-seguimiento-sellada-ropa"))
        .andExpect(content().string(containsString("No te quedan entregas para confirmar.")))
        .andExpect(content().string(not(containsString("Te queda una en la puerta"))))
        .andExpect(content().string(containsString("15 de octubre, 18:50")));
  }

  @Test
  void lasNecesidadesMuestranCadaEstadoYSusAcciones() throws Exception {
    mvc.perform(get("/entidad-necesidades"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("seccion", "Necesidades"))
        .andExpect(content().string(containsString("de 40 kg recibidos")))
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
            content().string(containsString("Otra persona de tu entidad cambió esta necesidad")));
    mvc.perform(get("/entidad-necesidades?escenario=entidad-inactiva"))
        .andExpect(content().string(containsString("Tu entidad no está activa")))
        .andExpect(content().string(not(containsString("Publicar necesidad"))));
    mvc.perform(get("/entidad-necesidades-publicar-error"))
        .andExpect(content().string(containsString("Revisá dos cosas antes de publicar")))
        .andExpect(content().string(containsString("aria-invalid=\"true\"")));
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
        .andExpect(content().string(containsString("pedido--nuevo")));
    mvc.perform(get("/entidad-necesidades-cambiada"))
        .andExpect(content().string(containsString("Guardamos los cambios de Lentejas secas.")))
        .andExpect(content().string(containsString("Cambiada hoy")))
        .andExpect(content().string(containsString("Hoy pasó de 40 a 45 kg por mes.")))
        .andExpect(content().string(containsString("de 45 kg recibidos")));
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
        .andExpect(content().string(containsString("de 20 litros recibidos")))
        .andExpect(content().string(containsString("Hasta el 20 de octubre")))
        .andExpect(content().string(not(containsString("20 litros · hasta"))))
        .andExpect(content().string(containsString("Desde junio de 2026")));
    mvc.perform(get("/entidad-necesidades?necesidad=ropa&escenario=sin-categoria"))
        .andExpect(content().string(containsString("Un donante · 5 unidades")))
        .andExpect(content().string(not(containsString("null"))));
    mvc.perform(get("/entidad-seguimiento-confirmar?entrega=ropa&escenario=sin-categoria"))
        .andExpect(content().string(containsString("Un donante")));
    mvc.perform(get("/entidad-seguimiento-confirmar?entrega=ropa"))
        .andExpect(content().string(containsString("Ropa · 10 unidades")))
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
          .andExpect(content().string(containsString("Ir a la portada")))
          .andExpect(content().string(not(containsString("otro.test"))));
    }
    mvc.perform(
            post("/publico-contacto-enviado")
                .param("volver", "/entidad-seguimiento-confirmar?entrega=lentejas"))
        .andExpect(
            redirectedUrl(
                "/publico-contacto-enviado?volver=%2Fentidad-seguimiento-confirmar%3Fentrega%3Dlentejas"));
    mvc.perform(get("/publico-contacto-enviado").param("volver", "/entidad-necesidades"))
        .andExpect(content().string(containsString("Volver a Necesidades")));
    mvc.perform(get("/publico-contacto-enviado").param("volver", "https://otro.test"))
        .andExpect(content().string(containsString("Ir a la portada")))
        .andExpect(content().string(not(containsString("otro.test"))));
  }

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
        .andExpect(content().string(containsString("Las entidades ya no ven tu nombre")))
        .andExpect(content().string(containsString("ven «Empresa donante · Sostenedor»")))
        .andExpect(content().string(containsString("la entidad ve tu categoría como donante")));
    mvc.perform(get("/donante-perfil-guardado?mostrar=si"))
        .andExpect(content().string(containsString("Las entidades ven tu nombre")));
    mvc.perform(post("/donante-perfil-guardado").param("seccion", "datos"))
        .andExpect(redirectedUrl("/donante-perfil-guardado?seccion=datos"));
    mvc.perform(post("/donante-perfil-guardado").param("seccion", "otra"))
        .andExpect(redirectedUrl("/donante-perfil-guardado"));
    mvc.perform(get("/donante-perfil-guardado?seccion=datos"))
        .andExpect(content().string(containsString("Guardamos los datos de tu cuenta")))
        .andExpect(content().string(not(containsString("Las entidades ya no ven tu nombre"))));
    mvc.perform(get("/donante-perfil-guardado?seccion=avisos"))
        .andExpect(content().string(containsString("Guardamos cómo te avisamos")));
    mvc.perform(get("/publico-legales"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Versión 1, vigente desde")))
        .andExpect(content().string(containsString("texto de ejemplo")))
        .andExpect(content().string(containsString("o solo en una donación al registrarla")))
        .andExpect(content().string(containsString("«Donante Sostenedor»")));
    mvc.perform(get("/entidad-seguimiento-sellada-ropa?escenario=sin-categoria"))
        .andExpect(content().string(containsString("Un donante")));
    mvc.perform(get("/publico-portada"))
        .andExpect(content().string(containsString("href=\"/publico-legales\"")))
        .andExpect(content().string(containsString("id=\"pausar-movimiento\"")));
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
        .andExpect(model().attribute("seccion", "Seguir entregas"))
        .andExpect(content().string(containsString("Contá qué <em>pasó</em>")))
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
    mvc.perform(get("/donante-inicio?respuesta=500")).andExpect(status().isInternalServerError());
    mvc.perform(get("/donante-inicio?respuesta=otra")).andExpect(status().isOk());
  }

  @Test
  void lasPaginasDeErrorDicenQuePasoYAQuienAvisar() throws Exception {
    String html = error(404, "/no-existe", "");
    assertTrue(html.contains("Error 404"), html);
    assertTrue(html.contains("No encontramos esta"));
    assertTrue(html.contains("Código para soporte: <code>a1b2c3d4e5f6</code>"));
    assertTrue(html.contains("href=\"/publico-contacto?codigo=a1b2c3d4e5f6\""));
    assertTrue(html.contains("class=\"raices-publico\""));
    html = error(403, "/entidad-inicio", "");
    assertTrue(html.contains("Esta página no es para tu"));
    assertTrue(html.contains("<title>Sin permiso - DonaTrack</title>"));
    html = error(500, "/donante-inicio", "");
    assertTrue(html.contains("Error 500"));
    assertTrue(html.contains("href=\"/donante-inicio\">Volver a intentar"), html);
    assertTrue(html.contains("Ir al\n          inicio"));
    html = error(500, "/donante-inicio", "sin-codigo");
    assertTrue(!html.contains("Código para soporte"));
    assertTrue(html.contains("Si sigue, escribinos y contanos qué estabas haciendo."));
  }

  /** La página de error que arma Spring Boot para ese código, como después de un sendError. */
  private String error(int codigo, String direccion, String escenario) throws Exception {
    return mvc.perform(
            get("/error")
                .param("escenario", escenario)
                .accept(MediaType.TEXT_HTML)
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, codigo)
                .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, direccion))
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
    mvc.perform(get("/publico-portada"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("href=\"/publico-ingresar\"")))
        .andExpect(content().string(containsString("href=\"/publico-mapa\"")))
        .andExpect(content().string(containsString("href=\"/publico-registro-donante\"")))
        .andExpect(content().string(containsString("href=\"/publico-registro-entidad\"")))
        .andExpect(content().string(not(containsString("href=\"/prototipo/"))));
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
        // El enlace a la landing de donantes, en la tarjeta de quien dona (usuario, 2026-11-08).
        .andExpect(content().string(containsString("Qué podés donar")))
        .andExpect(content().string(containsString("Cómo se suma una entidad")));
    mvc.perform(get("/publico-nosotros"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Entregas confirmadas desde marzo de 2026")))
        // Las cifras en árboles de K (2026-11-08; antes, la rama de frutos con data-cosecha).
        .andExpect(content().string(containsString("class=\"huerta-cifras\"")))
        .andExpect(content().string(containsString("Actualizado")))
        .andExpect(content().string(not(containsString("aria-current=\"page\""))));
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
          "entidad-recibidas",
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
        .andExpect(
            content().string(containsString("<caption class=\"solo-lectores\">Donantes</caption>")))
        .andExpect(content().string(containsString("scope=\"row\"")))
        .andExpect(content().string(containsString("data-rotulo=\"Documento\"")))
        .andExpect(
            content().string(containsString("href=\"/admin-donantes?importar=abrir#importar\"")));
    // Importar queda plegado; "Importar CSV" lo abre sin dejar la lista (RF-20).
    mvc.perform(get("/admin-donantes?importar=abrir"))
        .andExpect(content().string(containsString("enctype=\"multipart/form-data\"")))
        .andExpect(content().string(containsString("action=\"/admin-donantes-importando\"")))
        .andExpect(content().string(containsString("scope=\"row\"")));
    mvc.perform(get("/admin-donantes?escenario=vacio"))
        .andExpect(content().string(containsString("Todavía no hay donantes registrados.")));
    mvc.perform(get("/admin-donantes?escenario=lento"))
        .andExpect(content().string(containsString("aria-busy=\"true\"")));
    mvc.perform(get("/admin-donantes?escenario=error"))
        .andExpect(content().string(containsString("No pudimos cargar los donantes.")))
        .andExpect(content().string(containsString("role=\"alert\"")));
    mvc.perform(get("/admin-donantes-importando"))
        .andExpect(content().string(containsString("Procesando: 8.400 de 20.000 filas.")))
        .andExpect(content().string(containsString("Todos los donantes")));
    mvc.perform(get("/admin-donantes-importado"))
        .andExpect(content().string(containsString("19.100 donantes nuevos, 754 actualizados")))
        .andExpect(content().string(containsString("Falta el apellido.")));
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
        .andExpect(model().attribute("seccion", "Entidades"))
        .andExpect(content().string(containsString("Activas · 46")))
        .andExpect(content().string(containsString("Pendientes · 3")))
        .andExpect(content().string(containsString("Inactivas · 3")))
        .andExpect(content().string(containsString("Activa desde el 4 de febrero")));
    mvc.perform(get("/admin-entidades?ver=pendientes&entidad=merendero-este"))
        .andExpect(
            content().string(containsString("aria-label=\"Aprobar: Merendero Ejemplo Este\"")))
        .andExpect(content().string(containsString("popover")))
        .andExpect(content().string(containsString("Falta la constancia de")));
    mvc.perform(get("/admin-entidades?ver=inactivas"))
        .andExpect(content().string(containsString("Inactiva desde el 20 de agosto")));
    mvc.perform(get("/admin-entidades?escenario=vacio&ver=pendientes"))
        .andExpect(content().string(containsString("Pendientes · 0")))
        .andExpect(content().string(containsString("Ninguna entidad pide sumarse por ahora.")));
    mvc.perform(get("/admin-entidades?escenario=error"))
        .andExpect(content().string(containsString("No pudimos cargar las entidades.")));
    mvc.perform(get("/admin-entidades-aprobada"))
        .andExpect(content().string(containsString("Aprobaste a Hogar Ejemplo Norte.")))
        .andExpect(content().string(containsString("Pendientes · 2")))
        .andExpect(content().string(containsString("Activas · 47")))
        .andExpect(content().string(containsString("Activa desde el 15 de octubre")));
    mvc.perform(get("/admin-entidades-correccion"))
        .andExpect(content().string(containsString("Esperando")))
        .andExpect(content().string(containsString("Le pedimos a Merendero Ejemplo Este")))
        .andExpect(
            content().string(containsString("acciones vuelven cuando la entidad la reenvíe.")))
        .andExpect(content().string(not(containsString("Aprobar: Merendero Ejemplo Este"))));
    mvc.perform(get("/admin-entidades-rechazada"))
        .andExpect(
            content().string(containsString("Rechazaste la solicitud de Comedor Ejemplo Oeste.")))
        .andExpect(content().string(not(containsString("Comedor Ejemplo Oeste</b>"))));
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
        .andExpect(content().string(containsString("data-rotulo=\"Vence\"")))
        .andExpect(
            content().string(containsString("href=\"/admin-donaciones-detalle?donacion=leche\"")));
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
        .andExpect(content().string(containsString("Propuestas que esperan tu decisión · 2")))
        .andExpect(content().string(containsString("Cubre 10 de las 12 unidades")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "aria-label=\"Aprobar: propuesta para Comedor Barrio Ejemplo\"")))
        .andExpect(content().string(containsString("Decididas · 7")));
    mvc.perform(get("/admin-asignaciones?escenario=vacio"))
        .andExpect(content().string(containsString("No hay propuestas que esperen tu decisión.")));
    mvc.perform(get("/admin-asignaciones?escenario=corriendo"))
        .andExpect(content().string(containsString("Corriendo la asignación…")));
    mvc.perform(get("/admin-asignaciones-aprobada"))
        .andExpect(content().string(containsString("Aprobaste la propuesta para Comedor Barrio")))
        .andExpect(content().string(containsString("Propuestas que esperan tu decisión · 1")))
        .andExpect(content().string(containsString("Decididas · 8")));
    mvc.perform(get("/admin-asignaciones-descartada"))
        .andExpect(content().string(containsString("Descartaste la propuesta para Merendero")));
    mvc.perform(get("/admin-asignaciones-error"))
        .andExpect(content().string(containsString("ya no está activa")))
        .andExpect(
            content().string(not(containsString("Aprobar: propuesta para Merendero Ejemplo Sur"))));
    mvc.perform(get("/admin-asignaciones-error?escenario=propia"))
        .andExpect(content().string(containsString("de la misma entidad que la")));
    mvc.perform(get("/admin-asignaciones-error?escenario=cambio"))
        .andExpect(content().string(containsString("Volvimos a cargar la lista.")));
    mvc.perform(post("/admin-asignaciones-corrida"))
        .andExpect(redirectedUrl("/admin-asignaciones-corrida"));
    mvc.perform(get("/admin-asignaciones-corrida?escenario=nuevas"))
        .andExpect(content().string(containsString("generó 2 propuestas nuevas.")))
        .andExpect(content().string(containsString("Propuestas que esperan tu decisión · 4")));
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
  void elRankingDiceSuCriterioYSeFiltraPorTipoYPeriodo() throws Exception {
    mvc.perform(get("/admin-ranking"))
        .andExpect(model().attribute("seccion", "Ranking"))
        .andExpect(content().string(containsString("El ranking de octubre de 2026")))
        .andExpect(
            content().string(containsString("misiones que cada donante completó del 1 al 15")))
        .andExpect(content().string(containsString("hx-boost=\"true\"")))
        .andExpect(content().string(containsString("<caption>")));
    mvc.perform(get("/admin-ranking?periodo=anio&tipo=persona"))
        .andExpect(content().string(containsString("El ranking de 2026, solo personas")));
    mvc.perform(get("/admin-ranking?periodo=siempre"))
        .andExpect(content().string(containsString("desde marzo de 2025")));
    mvc.perform(get("/admin-ranking?escenario=vacio"))
        .andExpect(content().string(containsString("Todavía no hay ningún ranking")));
    mvc.perform(get("/admin-ranking?escenario=error"))
        .andExpect(content().string(containsString("No pudimos cargar el ranking.")));
  }

  private static final Pattern ENLACE = Pattern.compile("href=\"([^\"]*)\"");

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
        .andExpect(content().string(containsString("aria-label=\"Gran Aporte: 12 de 50 bienes\"")))
        .andExpect(content().string(containsString("Completa el 3 de agosto de 2026")))
        .andExpect(
            content().string(containsString("Empieza a contar cuando completes Gran Aporte.")))
        .andExpect(
            content()
                .string(containsString("Sus misiones se ven cuando completes las de Sostenedor.")))
        .andExpect(content().string(containsString("aria-label=\"Tus insignias\"")))
        .andExpect(content().string(containsString("(2 de 6)")))
        .andExpect(content().string(not(containsString("Notificaciones, 4 sin leer"))));
    mvc.perform(get("/donante-misiones?escenario=vacio").sessionAttr("cuenta", "persona"))
        .andExpect(content().string(containsString("Todavía no tenés insignias")))
        .andExpect(content().string(containsString("0 de 3 meses")))
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
        .andExpect(model().attribute("seccion", "Mis donaciones"))
        .andExpect(content().string(containsString("Martina Ruiz")));
    mvc.perform(get("/donante-notificaciones").sessionAttr("cuenta", "persona"))
        .andExpect(view().name("persona/donante-notificaciones"));
    mvc.perform(get("/donante-perfil").sessionAttr("cuenta", "persona"))
        .andExpect(view().name("persona/donante-perfil"))
        .andExpect(model().attribute("seccion", "Mi perfil"));
    // Una pantalla del donante sin versión de la persona sigue mostrando el aviso.
    mvc.perform(get("/donante-donaciones-nueva").sessionAttr("cuenta", "persona"))
        .andExpect(status().isOk())
        .andExpect(view().name("aviso-persona"))
        .andExpect(model().attribute("seccion", "Mis donaciones"))
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

  @Test
  void laEntidadSigueLoQueVieneASuPuerta() throws Exception {
    mvc.perform(get("/entidad-seguimiento"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("seccion", "Seguir entregas"))
        .andExpect(
            content().string(containsString("href=\"/entidad-seguimiento\" aria-current=\"page\"")))
        .andExpect(content().string(containsString("Lo que viene a tu puerta")))
        .andExpect(
            content().string(containsString("class=\"mapa-dibujo tierra-impresa\" aria-hidden")))
        .andExpect(content().string(containsString("estacion-ficha estacion-ficha--5")))
        .andExpect(content().string(not(containsString("estacion-ficha--6"))))
        .andExpect(content().string(containsString("Calle Ejemplo 450")))
        .andExpect(content().string(containsString("entrega=lentejas")))
        .andExpect(content().string(containsString("entrega=ropa")))
        .andExpect(content().string(containsString("Última noticia: 14 de octubre, 16:40.")))
        .andExpect(content().string(containsString("actualizada hoy a las 18:25")))
        .andExpect(content().string(containsString("href=\"/entidad-recibidas?estado=recibidas\"")))
        .andExpect(content().string(not(containsString("Confirmaste la recepción"))));
    mvc.perform(get("/entidad-seguimiento?escenario=vacio"))
        .andExpect(content().string(containsString("No tenés entregas por recibir.")))
        .andExpect(content().string(not(containsString("mapa-dibujo"))));
    mvc.perform(get("/entidad-seguimiento?escenario=lento"))
        .andExpect(content().string(containsString("role=\"status\">Cargando tus entregas")));
    mvc.perform(get("/entidad-seguimiento?escenario=error"))
        .andExpect(content().string(containsString("No pudimos cargar tus entregas.")))
        .andExpect(content().string(containsString("role=\"alert\"")));
  }

  @Test
  void elPerfilDeLaEntidadGuardaSusDatosYSusAvisos() throws Exception {
    mvc.perform(get("/entidad-perfil"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("seccion", "Mi perfil"))
        .andExpect(content().string(containsString("aria-label=\"Secciones de tu perfil\"")))
        .andExpect(content().string(containsString("Datos de la entidad")))
        .andExpect(content().string(containsString("Calle Ejemplo 450, Localidad Ejemplo")))
        .andExpect(content().string(containsString("No se cambia desde acá")))
        .andExpect(content().string(not(containsString("Cómo te ven"))))
        .andExpect(content().string(containsString("Aceptaste la versión 1")));
    mvc.perform(post("/entidad-perfil-guardado").param("seccion", "avisos"))
        .andExpect(redirectedUrl("/entidad-perfil-guardado?seccion=avisos"));
    mvc.perform(get("/entidad-perfil-guardado?seccion=datos"))
        .andExpect(content().string(containsString("Guardamos los datos de tu entidad.")));
    mvc.perform(get("/entidad-perfil-guardado?seccion=avisos"))
        .andExpect(content().string(containsString("Guardamos cómo te avisamos.")));
    mvc.perform(get("/entidad-perfil?escenario=lento"))
        .andExpect(content().string(containsString("Guardando…")));
    mvc.perform(get("/entidad-perfil?escenario=error"))
        .andExpect(content().string(containsString("No pudimos guardar los cambios.")));
    mvc.perform(get("/entidad-perfil?escenario=sin-conexion"))
        .andExpect(content().string(containsString("No sabemos si se guardó.")));
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
