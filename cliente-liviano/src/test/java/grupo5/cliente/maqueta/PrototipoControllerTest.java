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

/**
 * Las pantallas de prueba están en src/test/resources/templates/prototipo/cozy-medieval/ y
 * prototipo/raices/ y prototipo/raices-b/.
 */
@WebMvcTest(PrototipoController.class)
@Import(MaquetaConfig.class)
@ActiveProfiles("maqueta")
class PrototipoControllerTest {

  @Autowired private MockMvc mvc;

  @Test
  void unaPantallaDeNivelTresMarcaSuSeccionEnElMenuDeSuRol() throws Exception {
    mvc.perform(get("/prototipo/donante-donaciones-prueba"))
        .andExpect(status().isOk())
        .andExpect(view().name("prototipo/cozy-medieval/donante-donaciones-prueba"))
        .andExpect(model().attribute("alternativa", "cozy-medieval"))
        .andExpect(model().attribute("seccion", "Mis donaciones"))
        .andExpect(content().string(containsString("<p>Ana</p>")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "href=\"/prototipo/donante-donaciones\" aria-current=\"page\"")));
  }

  @Test
  void unaSeccionSinPantallaMuestraLaTierraSinExplorar() throws Exception {
    mvc.perform(get("/prototipo/admin-ranking"))
        .andExpect(status().isOk())
        .andExpect(view().name("prototipo/cozy-medieval/sin-dibujar"))
        .andExpect(model().attribute("seccion", "Ranking"))
        .andExpect(content().string(containsString("todavía no está dibujada")))
        .andExpect(content().string(containsString("href=\"/prototipo/admin-inicio\"")));
  }

  @Test
  void loQueNoEstaNiEnElMenuNiDibujadoNoExiste() throws Exception {
    mvc.perform(get("/prototipo/donante-tesoro")).andExpect(status().isNotFound());
    mvc.perform(get("/prototipo/..%2Fpaginas%2Festilos")).andExpect(status().isNotFound());
    mvc.perform(get("/prototipo/sin-dibujar")).andExpect(status().isNotFound());
    mvc.perform(get("/prototipo/perfil")).andExpect(status().isNotFound());
  }

  @Test
  void elPerfilQueEnlazaElLayoutMuestraLaTierraSinExplorar() throws Exception {
    mvc.perform(get("/prototipo/admin-perfil"))
        .andExpect(status().isOk())
        .andExpect(view().name("prototipo/cozy-medieval/sin-dibujar"))
        .andExpect(model().attribute("seccion", "Mi perfil"));
  }

  @Test
  void unFormularioVuelveASuDestinoSinLoEscrito() throws Exception {
    mvc.perform(post("/prototipo/donante-donaciones-prueba").param("contrasena", "x"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/prototipo/donante-donaciones-prueba"));
    mvc.perform(post("/prototipo/donante-tesoro")).andExpect(status().isNotFound());
  }

  @Test
  void raicesTieneSuPrototipoConElMenuEnSuDireccion() throws Exception {
    mvc.perform(get("/prototipo/raices/donante-donaciones-prueba"))
        .andExpect(status().isOk())
        .andExpect(view().name("prototipo/raices/donante-donaciones-prueba"))
        .andExpect(model().attribute("alternativa", "raices"))
        .andExpect(model().attribute("seccion", "Mis donaciones"))
        .andExpect(
            content()
                .string(
                    containsString(
                        "href=\"/prototipo/raices/donante-donaciones\" aria-current=\"page\"")));
  }

  @Test
  void enRaicesLoQueNoEstaDibujadoMuestraElAvisoOEnlazaACozy() throws Exception {
    mvc.perform(get("/prototipo/raices/donante-entidades-detalle?entidad=comedor"))
        .andExpect(status().isOk())
        .andExpect(view().name("prototipo/raices/sin-dibujar"))
        .andExpect(model().attribute("seccion", "Entidades"))
        .andExpect(content().string(containsString("Empresa donante")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "href=\"/prototipo/donante-entidades-detalle?entidad=comedor\"")));
    mvc.perform(get("/prototipo/raices/donante-incentivos")).andExpect(status().isNotFound());
    mvc.perform(get("/prototipo/raices/donante-tesoro")).andExpect(status().isNotFound());
    mvc.perform(get("/prototipo/raices/sin-dibujar")).andExpect(status().isNotFound());
    mvc.perform(post("/prototipo/raices/donante-tesoro")).andExpect(status().isNotFound());
  }

  @Test
  void elSeguimientoDeRaicesMuestraCadaEstado() throws Exception {
    mvc.perform(get("/prototipo/raices/donante-seguimiento"))
        .andExpect(status().isOk())
        .andExpect(view().name("prototipo/raices/donante-seguimiento"))
        .andExpect(model().attribute("seccion", "Seguimiento"))
        .andExpect(content().string(containsString("3 entregas en curso")))
        .andExpect(content().string(containsString("aria-current=\"step\"")))
        .andExpect(content().string(containsString("aria-labelledby=\"lista-titulo\"")));
    mvc.perform(get("/prototipo/raices/donante-seguimiento?escenario=vacio"))
        .andExpect(
            content().string(containsString("Ningún camión está en camino con tus donaciones")));
    mvc.perform(get("/prototipo/raices/donante-seguimiento?escenario=error"))
        .andExpect(content().string(containsString("role=\"alert\"")))
        .andExpect(content().string(containsString("Volver a intentar")));
    mvc.perform(get("/prototipo/raices/donante-seguimiento?escenario=lento"))
        .andExpect(content().string(containsString("aria-busy=\"true\"")));
  }

  @Test
  void elDonanteDeRaicesRecorreSuInicioSusDonacionesEntidadesYNotificaciones() throws Exception {
    mvc.perform(get("/prototipo/raices/donante-inicio"))
        .andExpect(status().isOk())
        .andExpect(view().name("prototipo/raices/donante-inicio"))
        .andExpect(content().string(containsString("Primera entrega confirmada: fideos secos")))
        .andExpect(content().string(containsString("aria-label=\"Notificaciones, 4 sin leer\"")));
    mvc.perform(get("/prototipo/raices/donante-inicio?escenario=lento"))
        .andExpect(content().string(containsString("aria-busy=\"true\"")));
    mvc.perform(get("/prototipo/raices/donante-donaciones?estado=pendiente&categoria=alimentos"))
        .andExpect(content().string(containsString("2 donaciones con esos filtros.")));
    mvc.perform(get("/prototipo/raices/donante-donaciones?estado=otro"))
        .andExpect(content().string(containsString("7 donaciones, de la más reciente")));
    mvc.perform(get("/prototipo/raices/donante-donaciones-detalle?donacion=ropa"))
        .andExpect(model().attribute("seccion", "Mis donaciones"))
        .andExpect(content().string(containsString("confirmó la entrega el 9 de octubre")));
    mvc.perform(get("/prototipo/raices/donante-donaciones-detalle?donacion=pan"))
        .andExpect(content().string(containsString("No encontramos esa donación.")));
    mvc.perform(get("/prototipo/raices/donante-entidades?nombre=zzz"))
        .andExpect(content().string(containsString("Ninguna entidad activa se llama así.")));
    mvc.perform(get("/prototipo/raices/donante-entidades?escenario=error"))
        .andExpect(content().string(containsString("role=\"alert\"")))
        .andExpect(content().string(containsString("Cómo llega tu donación")));
    mvc.perform(post("/prototipo/raices/donante-notificaciones-leidas"))
        .andExpect(redirectedUrl("/prototipo/raices/donante-notificaciones-leidas"));
    mvc.perform(get("/prototipo/raices/donante-notificaciones-leidas"))
        .andExpect(content().string(containsString("Listo: marcaste todas como leídas.")))
        .andExpect(content().string(containsString("aria-label=\"Notificaciones\"")));
  }

  @Test
  void elActaDeRaicesMuestraCadaEstadoYConfirmaConUnFormularioReal() throws Exception {
    mvc.perform(get("/prototipo/raices/entidad-seguimiento-confirmar"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("seccion", "Seguir entregas"))
        .andExpect(
            content().string(containsString("Lentejas secas, 15 kg. Llegó hoy a las 18:20.")))
        .andExpect(content().string(containsString("enctype=\"multipart/form-data\"")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "action=\"/prototipo/raices/entidad-seguimiento-sellada-lentejas\"")))
        .andExpect(content().string(containsString("aria-current=\"step\"")))
        .andExpect(content().string(containsString("name=\"fotos\"")));
    mvc.perform(get("/prototipo/raices/entidad-seguimiento-confirmar?entrega=ropa"))
        .andExpect(content().string(containsString("Donó</dt>")))
        .andExpect(content().string(containsString("Donante Sostenedor")));
    mvc.perform(get("/prototipo/raices/entidad-seguimiento-confirmar?entrega=pan"))
        .andExpect(content().string(containsString("No encontramos esa entrega")));
    mvc.perform(get("/prototipo/raices/entidad-seguimiento-confirmar?escenario=confirmada"))
        .andExpect(content().string(containsString("Ya confirmaste esta entrega")))
        .andExpect(content().string(containsString("Esta entrega ya está <em>confirmada</em>")))
        .andExpect(content().string(not(containsString("Van 10 kg recibidos."))));
    mvc.perform(get("/prototipo/raices/entidad-seguimiento-confirmar?escenario=sin-conexion"))
        .andExpect(content().string(containsString("No sabemos si la confirmación llegó.")));
    mvc.perform(get("/prototipo/raices/entidad-seguimiento-confirmar-error"))
        .andExpect(content().string(containsString("Revisá dos cosas antes de confirmar")))
        .andExpect(content().string(containsString("aria-invalid=\"true\"")));
  }

  @Test
  void confirmarEnRaicesLlevaALaRaizCompletaYALaQueSigue() throws Exception {
    mvc.perform(post("/prototipo/raices/entidad-seguimiento-sellada-lentejas"))
        .andExpect(redirectedUrl("/prototipo/raices/entidad-seguimiento-sellada-lentejas"));
    mvc.perform(get("/prototipo/raices/entidad-seguimiento-sellada-lentejas"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("confirmada--recien")))
        .andExpect(content().string(containsString("Te queda una en la puerta")))
        .andExpect(content().string(containsString("entidad-seguimiento-confirmar?entrega=ropa")));
    mvc.perform(get("/prototipo/raices/entidad-seguimiento-sellada-ropa"))
        .andExpect(content().string(containsString("No te quedan entregas para confirmar.")))
        .andExpect(content().string(not(containsString("Te queda una en la puerta"))))
        .andExpect(content().string(containsString("15 de octubre, 18:50")));
  }

  @Test
  void lasNecesidadesDeRaicesMuestranCadaEstadoYSusAcciones() throws Exception {
    mvc.perform(get("/prototipo/raices/entidad-necesidades"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("seccion", "Necesidades"))
        .andExpect(content().string(containsString("10 de 40 kg recibidos")))
        .andExpect(content().string(containsString("Llegó una parte")))
        .andExpect(content().string(containsString("aria-current=\"true\"")))
        .andExpect(content().string(containsString("popover")))
        .andExpect(
            content()
                .string(
                    containsString("action=\"/prototipo/raices/entidad-necesidades-publicada\"")))
        .andExpect(content().string(containsString("Cumplidas y terminadas (3)")));
    mvc.perform(get("/prototipo/raices/entidad-necesidades?escenario=vacio"))
        .andExpect(content().string(containsString("No tenés necesidades vigentes.")));
    mvc.perform(get("/prototipo/raices/entidad-necesidades?escenario=error"))
        .andExpect(content().string(containsString("No pudimos cargar tus necesidades.")))
        .andExpect(content().string(containsString("role=\"alert\"")));
    mvc.perform(get("/prototipo/raices/entidad-necesidades?escenario=lento"))
        .andExpect(content().string(containsString("aria-busy=\"true\"")));
    mvc.perform(get("/prototipo/raices/entidad-necesidades?necesidad=pan"))
        .andExpect(content().string(containsString("No encontramos esa necesidad.")));
    mvc.perform(get("/prototipo/raices/entidad-necesidades?escenario=conflicto"))
        .andExpect(
            content().string(containsString("Otra persona de tu entidad cambió este pedido")));
    mvc.perform(get("/prototipo/raices/entidad-necesidades?escenario=entidad-inactiva"))
        .andExpect(content().string(containsString("Tu entidad no está activa")))
        .andExpect(content().string(not(containsString("Publicar necesidad"))));
    mvc.perform(get("/prototipo/raices/entidad-necesidades-publicar-error"))
        .andExpect(content().string(containsString("Revisá dos cosas antes de publicar")))
        .andExpect(content().string(containsString("aria-invalid=\"true\"")));
  }

  @Test
  void cadaAccionDeNecesidadesEnRaicesLlevaASuAviso() throws Exception {
    mvc.perform(post("/prototipo/raices/entidad-necesidades-baja"))
        .andExpect(redirectedUrl("/prototipo/raices/entidad-necesidades-baja"));
    mvc.perform(post("/prototipo/raices/entidad-necesidades-publicada"))
        .andExpect(redirectedUrl("/prototipo/raices/entidad-necesidades-publicada"));
    mvc.perform(post("/prototipo/raices/entidad-necesidades-cambiada"))
        .andExpect(redirectedUrl("/prototipo/raices/entidad-necesidades-cambiada"));
    mvc.perform(get("/prototipo/raices/entidad-necesidades-baja"))
        .andExpect(content().string(containsString("Diste de baja Aceite de girasol.")))
        .andExpect(content().string(containsString("Cumplidas y terminadas (4)")));
    mvc.perform(get("/prototipo/raices/entidad-necesidades-publicada"))
        .andExpect(content().string(containsString("Publicamos tu necesidad.")))
        .andExpect(content().string(containsString("pedido--nuevo")));
    mvc.perform(get("/prototipo/raices/entidad-necesidades-cambiada"))
        .andExpect(content().string(containsString("Guardamos los cambios de Lentejas secas.")))
        .andExpect(content().string(containsString("Cambiada hoy")))
        .andExpect(content().string(containsString("Hoy pasó de 40 a 45 kg por mes.")))
        .andExpect(content().string(containsString("10 de 45 kg recibidos")));
  }

  @Test
  void lasNecesidadesDeRaicesSiguenLoDecididoConElGrupo() throws Exception {
    mvc.perform(get("/prototipo/raices/entidad-necesidades?necesidad=arroz"))
        .andExpect(content().string(containsString("Cada quincena")))
        .andExpect(content().string(containsString("Fideos secos (kg)")))
        .andExpect(content().string(containsString("Cuánto, en kg")))
        .andExpect(content().string(containsString("Vale desde este período, octubre")))
        .andExpect(content().string(containsString("1 entrega ya salió del depósito")))
        .andExpect(content().string(containsString("Martina Ruiz · 5 kg")));
    mvc.perform(get("/prototipo/raices/entidad-necesidades?necesidad=leche"))
        .andExpect(content().string(containsString("1 entrega todavía no salió")));
    mvc.perform(get("/prototipo/raices/entidad-necesidades?necesidad=ropa"))
        .andExpect(content().string(containsString("Donante Sostenedor · 5 unidades")))
        .andExpect(content().string(containsString("Ver los anteriores")))
        .andExpect(content().string(containsString("0 de 20 litros recibidos")))
        .andExpect(content().string(containsString("Hasta el 20 de octubre")))
        .andExpect(content().string(not(containsString("20 litros · hasta"))))
        .andExpect(content().string(containsString("Desde junio de 2026")));
    mvc.perform(get("/prototipo/raices/entidad-necesidades?necesidad=ropa&escenario=sin-categoria"))
        .andExpect(content().string(containsString("Un donante · 5 unidades")))
        .andExpect(content().string(not(containsString("null"))));
    mvc.perform(
            get(
                "/prototipo/raices/entidad-seguimiento-confirmar?entrega=ropa&escenario=sin-categoria"))
        .andExpect(content().string(containsString("Un donante")));
    mvc.perform(get("/prototipo/raices/entidad-seguimiento-confirmar?entrega=ropa"))
        .andExpect(content().string(containsString("Ropa · 10 unidades")))
        .andExpect(content().string(not(containsString("bultos"))));
  }

  @Test
  void elContactoDeRaicesLlegaConElCodigoYVuelveASuPantalla() throws Exception {
    mvc.perform(
            get("/prototipo/raices/publico-contacto")
                .param("codigo", "a1b2c3d4e5f6")
                .param("volver", "/prototipo/raices/entidad-necesidades"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("value=\"a1b2c3d4e5f6\"")))
        .andExpect(content().string(containsString("name=\"correo\"")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "action=\"/prototipo/raices/publico-contacto-enviado?volver=/prototipo/raices/entidad-necesidades\"")))
        .andExpect(content().string(containsString("Términos y privacidad")));
    mvc.perform(get("/prototipo/raices/publico-contacto?escenario=con-sesion"))
        .andExpect(content().string(containsString("el correo de tu cuenta")))
        .andExpect(content().string(not(containsString("name=\"correo\""))));
    mvc.perform(get("/prototipo/raices/publico-contacto-error"))
        .andExpect(content().string(containsString("Revisá dos cosas antes de enviar")))
        .andExpect(content().string(containsString("aria-invalid=\"true\"")));
    mvc.perform(
            post("/prototipo/raices/publico-contacto-enviado")
                .param("volver", "/prototipo/raices/entidad-necesidades")
                .param("mensaje", "x"))
        .andExpect(
            redirectedUrl(
                "/prototipo/raices/publico-contacto-enviado?volver=%2Fprototipo%2Fraices%2Fentidad-necesidades"));
    mvc.perform(
            post("/prototipo/raices/publico-contacto-enviado").param("volver", "https://otro.test"))
        .andExpect(redirectedUrl("/prototipo/raices/publico-contacto-enviado"));
    mvc.perform(
            post("/prototipo/raices/publico-contacto-enviado")
                .param("volver", "/prototipo/raices/../x"))
        .andExpect(redirectedUrl("/prototipo/raices/publico-contacto-enviado"));
    mvc.perform(
            get("/prototipo/raices/publico-contacto-enviado")
                .param("volver", "/prototipo/raices/entidad-necesidades"))
        .andExpect(content().string(containsString("Volver a Necesidades")));
    mvc.perform(
            get("/prototipo/raices/publico-contacto-enviado").param("volver", "https://otro.test"))
        .andExpect(content().string(containsString("Ir a la portada")))
        .andExpect(content().string(not(containsString("otro.test"))));
  }

  @Test
  void miPerfilEnRaicesEligeSiSeVeElNombreYMuestraLosLegales() throws Exception {
    mvc.perform(get("/prototipo/raices/donante-perfil"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("seccion", "Mi perfil"))
        .andExpect(
            content()
                .string(
                    containsString(
                        "href=\"/prototipo/raices/donante-perfil\" aria-current=\"page\"")))
        .andExpect(content().string(containsString("Donó: Distribuidora Ejemplo SA")))
        .andExpect(content().string(containsString("Guardar mi elección")))
        .andExpect(content().string(containsString("Aceptaste la versión 1")));
    mvc.perform(post("/prototipo/raices/donante-perfil-guardado").param("mostrar", "si"))
        .andExpect(redirectedUrl("/prototipo/raices/donante-perfil-guardado?mostrar=si"));
    mvc.perform(post("/prototipo/raices/donante-perfil-guardado").param("mostrar", "no"))
        .andExpect(redirectedUrl("/prototipo/raices/donante-perfil-guardado"));
    mvc.perform(get("/prototipo/raices/donante-perfil-guardado"))
        .andExpect(content().string(containsString("Las entidades ya no ven tu nombre")))
        .andExpect(content().string(containsString("ven «Empresa donante · Sostenedor»")))
        .andExpect(content().string(containsString("la entidad ve tu categoría como donante")));
    mvc.perform(get("/prototipo/raices/donante-perfil-guardado?mostrar=si"))
        .andExpect(content().string(containsString("Las entidades ven tu nombre")));
    mvc.perform(get("/prototipo/raices/publico-legales"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Versión 1, vigente desde")))
        .andExpect(content().string(containsString("texto de ejemplo")))
        .andExpect(content().string(containsString("o solo en una donación al registrarla")))
        .andExpect(content().string(containsString("«Donante Sostenedor»")));
    mvc.perform(get("/prototipo/raices/entidad-seguimiento-sellada-ropa?escenario=sin-categoria"))
        .andExpect(content().string(containsString("Un donante")));
    mvc.perform(get("/prototipo/raices/publico-portada"))
        .andExpect(content().string(containsString("href=\"/prototipo/raices/publico-legales\"")))
        .andExpect(content().string(containsString("id=\"pausar-movimiento\"")));
  }

  @Test
  void loQueTampocoDibujoCozyNoEnlazaACozy() throws Exception {
    mvc.perform(get("/prototipo/raices/entidad-seguimiento-problema?entrega=lentejas"))
        .andExpect(status().isOk())
        .andExpect(view().name("prototipo/raices/sin-dibujar"))
        .andExpect(model().attribute("enCozy", (Object) null))
        .andExpect(content().string(containsString("Volver a la entrega")));
  }

  @Test
  void unFormularioDeRaicesVuelveARaices() throws Exception {
    mvc.perform(post("/prototipo/raices/donante-donaciones-prueba"))
        .andExpect(redirectedUrl("/prototipo/raices/donante-donaciones-prueba"));
  }

  @Test
  void laPortadaDeRaicesEnlazaSoloARaices() throws Exception {
    mvc.perform(get("/prototipo/raices/publico-portada"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("href=\"/prototipo/raices/publico-ingresar\"")))
        .andExpect(content().string(containsString("href=\"/prototipo/raices/publico-mapa\"")))
        .andExpect(
            content().string(containsString("href=\"/prototipo/raices/publico-registro-donante\"")))
        .andExpect(
            content().string(containsString("href=\"/prototipo/raices/publico-registro-entidad\"")))
        .andExpect(content().string(not(containsString("href=\"/prototipo/publico-"))));
  }

  @Test
  void raicesBTieneSuPortadaYEnlazaSoloASuSitio() throws Exception {
    mvc.perform(get("/prototipo/raices-b/publico-portada"))
        .andExpect(status().isOk())
        .andExpect(view().name("prototipo/raices-b/publico-portada"))
        .andExpect(model().attribute("alternativa", "raices-b"))
        .andExpect(content().string(containsString("href=\"/css/prototipo/raices-b/")))
        .andExpect(
            content().string(containsString("href=\"/prototipo/raices-b/publico-ingresar\"")))
        .andExpect(content().string(containsString("href=\"/prototipo/raices-b/publico-legales\"")))
        .andExpect(content().string(not(containsString("href=\"/prototipo/raices/"))))
        .andExpect(content().string(not(containsString("href=\"/prototipo/publico-"))));
  }

  @Test
  void enRaicesBLoNoDibujadoYLosFormulariosSeQuedanEnSuSitio() throws Exception {
    mvc.perform(get("/prototipo/raices-b/admin-donantes"))
        .andExpect(status().isOk())
        .andExpect(view().name("prototipo/raices-b/sin-dibujar"))
        .andExpect(model().attribute("seccion", "Donantes"));
    mvc.perform(get("/prototipo/raices-b/donante-incentivos")).andExpect(status().isNotFound());
    mvc.perform(get("/prototipo/raices-b/sin-dibujar")).andExpect(status().isNotFound());
    mvc.perform(post("/prototipo/raices-b/publico-portada").param("volver", "/prototipo/raices/x"))
        .andExpect(redirectedUrl("/prototipo/raices-b/publico-portada"));
    mvc.perform(
            post("/prototipo/raices-b/publico-portada").param("volver", "/prototipo/raices-b/x"))
        .andExpect(
            redirectedUrl(
                "/prototipo/raices-b/publico-portada?volver=%2Fprototipo%2Fraices-b%2Fx"));
    mvc.perform(post("/prototipo/raices-b/donante-tesoro")).andExpect(status().isNotFound());
  }

  @Test
  void enRaicesBLaNavegacionEsUnaLineaPublicaYOtraPorRol() throws Exception {
    mvc.perform(get("/prototipo/raices-b/publico-portada"))
        .andExpect(model().attribute("perfil", (Object) null))
        .andExpect(
            content().string(containsString("href=\"/prototipo/raices-b/publico-donaciones\"")))
        .andExpect(content().string(containsString("href=\"/prototipo/raices-b/publico-mapa\"")))
        .andExpect(
            content().string(containsString("href=\"/prototipo/raices-b/publico-entidades\"")))
        .andExpect(
            content().string(containsString("href=\"/prototipo/raices-b/publico-registro\"")));
    // Lo público sin plantilla no existe: no cae en "sin dibujar", que es privada.
    mvc.perform(get("/prototipo/raices-b/publico-perfil")).andExpect(status().isNotFound());
    mvc.perform(get("/prototipo/raices/publico-perfil")).andExpect(status().isNotFound());
    mvc.perform(get("/prototipo/raices-b/donante-seguimiento"))
        .andExpect(
            content()
                .string(
                    containsString(
                        "href=\"/prototipo/raices-b/donante-seguimiento\" aria-current=\"page\"")))
        .andExpect(
            content()
                .string(containsString("href=\"/prototipo/raices-b/donante-notificaciones\"")));
    mvc.perform(get("/prototipo/raices-b/admin-inicio"))
        .andExpect(content().string(not(containsString("-notificaciones\""))));
  }

  /** Recuperar la contraseña: el aviso no revela cuentas ni promete un plazo (RNF-AUTH-10, D8). */
  @Test
  void enLosDosSitiosDeRaicesRecuperarLaContrasenaAvisaSinRevelarCuentas() throws Exception {
    for (String sitio : List.of("raices", "raices-b")) {
      String enviado = PROTOTIPO + sitio + "/publico-recuperar-enviado";
      mvc.perform(post(enviado)).andExpect(redirectedUrl(enviado));
      mvc.perform(get(enviado))
          .andExpect(status().isOk())
          .andExpect(
              content().string(containsString("Si hay una cuenta con ese correo, te mandamos")))
          .andExpect(content().string(not(containsString("vence en"))));
    }
  }

  /** El sitio A no toca las siete pantallas aprobadas antes de la corrida A/B (encargo, §1). */
  @Test
  void enRaicesALasPantallasAprobadasNoSumanNavegacion() throws Exception {
    for (String pantalla :
        List.of(
            "donante-perfil",
            "donante-seguimiento",
            "entidad-necesidades",
            "entidad-seguimiento-confirmar")) {
      mvc.perform(get("/prototipo/raices/" + pantalla))
          .andExpect(status().isOk())
          .andExpect(content().string(not(containsString("armazon-avisos"))));
    }
    for (String pantalla : List.of("publico-contacto", "publico-legales")) {
      mvc.perform(get("/prototipo/raices/" + pantalla))
          .andExpect(status().isOk())
          .andExpect(content().string(not(containsString("publica-menu"))))
          .andExpect(content().string(not(containsString("raices-publico.js"))));
    }
    mvc.perform(get("/prototipo/raices/donante-donaciones"))
        .andExpect(content().string(containsString("armazon-avisos")));
    mvc.perform(get("/prototipo/raices/publico-mapa"))
        .andExpect(content().string(containsString("publica-menu")));
  }

  @Test
  void loQueEnlazaElLayoutPrivadoLlevaAlPrototipo() throws Exception {
    mvc.perform(get("/salir")).andExpect(redirectedUrl("/prototipo/publico-portada"));
    mvc.perform(get("/notificaciones")).andExpect(redirectedUrl("/prototipo/donante-cartas"));
    mvc.perform(get("/perfil")).andExpect(redirectedUrl("/prototipo/donante-perfil"));
  }

  /** Las pantallas que los dos sitios de Raíces dibujan de verdad (no caen en "sin dibujar"). */
  private static final List<String> DIBUJADAS_EN_RAICES =
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
  private static final String PROTOTIPO = "/prototipo/";

  @Test
  void enRaicesAEstanDibujadasLasPantallasDeLosTresRecorridos() throws Exception {
    exigirDibujadas("raices");
  }

  @Test
  void enRaicesBEstanDibujadasLasPantallasDeLosTresRecorridos() throws Exception {
    exigirDibujadas("raices-b");
  }

  @Test
  void ningunEnlaceDeRaicesAEstaRoto() throws Exception {
    recorrerEnAnchura("raices");
  }

  @Test
  void ningunEnlaceDeRaicesBEstaRoto() throws Exception {
    recorrerEnAnchura("raices-b");
  }

  private void exigirDibujadas(String sitio) throws Exception {
    for (String pantalla : DIBUJADAS_EN_RAICES) {
      mvc.perform(get("/prototipo/" + sitio + "/" + pantalla))
          .andExpect(status().isOk())
          .andExpect(view().name("prototipo/" + sitio + "/" + pantalla));
    }
  }

  /**
   * Desde las portadas de cada rol sigue, en anchura, cada enlace que queda dentro del sitio: todos
   * responden 200 y, salvo en "sin dibujar", ninguno sale a otra alternativa. El enlace roto se
   * arregla en la plantilla, no acá.
   */
  private void recorrerEnAnchura(String sitio) throws Exception {
    String prefijo = PROTOTIPO + sitio + "/";
    Deque<String> pendientes = new ArrayDeque<>();
    Set<String> vistos = new LinkedHashSet<>();
    for (String inicio :
        List.of("publico-portada", "donante-inicio", "entidad-inicio", "admin-inicio")) {
      pendientes.add(prefijo + inicio);
      vistos.add(prefijo + inicio);
    }
    while (!pendientes.isEmpty()) {
      String direccion = pendientes.poll();
      MvcResult resultado =
          mvc.perform(get(URI.create(direccion))).andExpect(status().isOk()).andReturn();
      String vista = resultado.getModelAndView().getViewName();
      boolean enUnRecorrido = !("prototipo/" + sitio + "/sin-dibujar").equals(vista);
      Matcher enlace = ENLACE.matcher(resultado.getResponse().getContentAsString());
      while (enlace.find()) {
        String destino = enlace.group(1).replace("&amp;", "&");
        int ancla = destino.indexOf('#');
        if (ancla >= 0) {
          destino = destino.substring(0, ancla);
        }
        // Desde los recorridos, ningún enlace sale a otra alternativa: solo "sin dibujar" ofrece
        // ver la pantalla en Cozy (decisión del usuario, 2026-11-01).
        assertTrue(
            !enUnRecorrido || !destino.startsWith(PROTOTIPO) || destino.startsWith(prefijo),
            direccion + " enlaza fuera de " + sitio + ": " + destino);
        if (destino.startsWith(prefijo) && vistos.add(destino)) {
          pendientes.add(destino);
        }
      }
    }
    assertTrue(vistos.size() > 40, "el recorrido de " + sitio + " llegó a pocas pantallas");
  }
}
