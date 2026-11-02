package grupo5.cliente.maqueta;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Las pantallas de prueba están en src/test/resources/templates/prototipo/cozy-medieval/ y
 * prototipo/raices/.
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
    mvc.perform(get("/prototipo/raices/donante-donaciones-detalle?donacion=leche"))
        .andExpect(status().isOk())
        .andExpect(view().name("prototipo/raices/sin-dibujar"))
        .andExpect(model().attribute("seccion", "Mis donaciones"))
        .andExpect(content().string(containsString("Empresa donante")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "href=\"/prototipo/donante-donaciones-detalle?donacion=leche\"")));
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
        .andExpect(content().string(containsString("Una persona")));
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
    mvc.perform(get("/prototipo/raices/entidad-necesidades-baja"))
        .andExpect(content().string(containsString("Diste de baja Aceite de girasol.")))
        .andExpect(content().string(containsString("Cumplidas y terminadas (4)")));
    mvc.perform(get("/prototipo/raices/entidad-necesidades-publicada"))
        .andExpect(content().string(containsString("Publicamos tu necesidad.")))
        .andExpect(content().string(containsString("pedido--nuevo")));
    mvc.perform(get("/prototipo/raices/entidad-necesidades-cambiada"))
        .andExpect(content().string(containsString("Guardamos los cambios de Lentejas secas.")));
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
  void loQueEnlazaElLayoutPrivadoLlevaAlPrototipo() throws Exception {
    mvc.perform(get("/salir")).andExpect(redirectedUrl("/prototipo/publico-portada"));
    mvc.perform(get("/notificaciones")).andExpect(redirectedUrl("/prototipo/donante-cartas"));
    mvc.perform(get("/perfil")).andExpect(redirectedUrl("/prototipo/donante-perfil"));
  }
}
