package grupo5.cliente.maqueta;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

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
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** La pantalla de prueba está en src/test/resources/templates/. */
@WebMvcTest(SitioController.class)
@Import(MaquetaConfig.class)
@ActiveProfiles("maqueta")
class SitioControllerTest {

  @Autowired private MockMvc mvc;

  @Test
  void elInicioEsLaPortada() throws Exception {
    mvc.perform(get("/")).andExpect(status().isOk()).andExpect(view().name("publico-portada"));
  }

  @Test
  void unaSeccionSinPantallaMuestraQueNoEstaDibujada() throws Exception {
    mvc.perform(get("/admin-ranking"))
        .andExpect(status().isOk())
        .andExpect(view().name("sin-dibujar"))
        .andExpect(model().attribute("seccion", "Ranking"))
        .andExpect(content().string(containsString("todavía no está dibujada")))
        .andExpect(content().string(containsString("href=\"/admin-inicio\"")));
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
  void elPerfilQueEnlazaElLayoutMuestraQueNoEstaDibujado() throws Exception {
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
        .andExpect(content().string(containsString("10 de 40 kg recibidos")))
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
            content().string(containsString("Otra persona de tu entidad cambió este pedido")));
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
        .andExpect(content().string(containsString("10 de 45 kg recibidos")));
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
        .andExpect(content().string(containsString("0 de 20 litros recibidos")))
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
  void unaEntregaSinPantallaVuelveALaEntrega() throws Exception {
    mvc.perform(get("/entidad-seguimiento-problema?entrega=lentejas"))
        .andExpect(status().isOk())
        .andExpect(view().name("sin-dibujar"))
        .andExpect(content().string(containsString("Volver a la entrega")));
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
          .andExpect(content().string(containsString("publica-menu")))
          .andExpect(content().string(not(containsString("aria-current=\"page\""))));
    }
    mvc.perform(get("/donante-donaciones"))
        .andExpect(content().string(containsString("armazon-avisos")));
    mvc.perform(get("/publico-mapa")).andExpect(content().string(containsString("publica-menu")));
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
          "donante-inicio",
          "donante-donaciones",
          "donante-donaciones-detalle",
          "donante-entidades",
          "donante-notificaciones",
          "donante-seguimiento",
          "donante-perfil",
          "entidad-inicio",
          "entidad-recibidas",
          "entidad-notificaciones",
          "entidad-necesidades",
          "entidad-seguimiento-confirmar",
          "admin-inicio");

  private static final Pattern ENLACE = Pattern.compile("href=\"([^\"]*)\"");

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
