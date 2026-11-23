package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.MaquetaConfig;
import grupo5.cliente.puertos.EntidadSeguimientoPuerto;
import grupo5.cliente.puertos.RecibidasPuerto;
import grupo5.cliente.vistas.RecibidasVista;
import grupo5.cliente.vistas.RecibidasVista.Recibida;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Entregas (DA-02) y la entrega con problema (ORG-08) con lo que dan Donaciones y Logística: sin
 * pedido que cubre, una sin categoría, sin el aviso de novedades y con el mes de las confirmadas;
 * la entidad llega por {@code ?entidad=}.
 */
@WebMvcTest(
    controllers = {EntidadSeguimientoController.class, PantallasController.class},
    properties = "cliente.datos.cuenta=parametro")
@ActiveProfiles("maqueta")
@Import({
  MaquetaConfig.class,
  Pantallas.class,
  CuentaPorParametro.class,
  EntidadSeguimientoControllerTest.DeLasApis.class
})
class EntidadSeguimientoControllerTest {

  private static final String PANTALLA = "/entidad-seguimiento";
  private static final String PROBLEMA = "/entidad-entrega-problema";
  private static final String ENTIDAD = "6f1c2c1e-0000-4000-8000-0000000000e1";

  /** La entidad recién aprobada: sin nada asignado. */
  private static final UUID VACIA = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000f0");

  @TestConfiguration
  static class DeLasApis {

    @Bean
    RecibidasPuerto recibidas() {
      return entidad ->
          new RecibidasVista(
              VACIA.equals(entidad)
                  ? List.of()
                  : List.of(
                      new Recibida(
                          "d1",
                          "Silla plegable",
                          null,
                          null,
                          "1",
                          "un donante",
                          null,
                          null,
                          "camino",
                          "Salió del depósito el 8 de noviembre a las 9:15.",
                          null,
                          null,
                          null,
                          "salió del depósito el 8 de noviembre"),
                      new Recibida(
                          "p1",
                          "Aceite de girasol",
                          "cat-alimentos",
                          "Alimentos",
                          "20 litros",
                          "Almacén Ejemplo SRL",
                          "n1",
                          "aceite de girasol",
                          "problema",
                          "El camión no pudo llegar el 7 de noviembre.",
                          null,
                          null,
                          null,
                          "no pudo llegar el 7 de noviembre"),
                      new Recibida(
                          "b",
                          "Fideos secos",
                          "cat-alimentos",
                          "Alimentos",
                          "8 kg",
                          "Martina Ruiz",
                          null,
                          null,
                          "recibida",
                          "Confirmaste la recepción el 5 de noviembre.",
                          "5",
                          "2026-11-05",
                          null,
                          null)),
              "noviembre",
              "8 de noviembre de 2026, 18:30",
              null);
    }

    @Bean
    EntidadSeguimientoPuerto seguimientos() {
      return new EntidadSeguimientoPuerto() {
        @Override
        public Map<String, String> noticias(UUID entidad, Collection<String> claves) {
          return Map.of(
              "d1", "Última noticia: 8 de noviembre, 18:25.",
              "p1", "Última noticia: 7 de noviembre, 16:40.");
        }

        @Override
        public boolean dibujoDeEjemplo() {
          return false;
        }
      };
    }
  }

  @Autowired private MockMvc mvc;

  @Test
  void sinEntidadMandaAIngresar() throws Exception {
    mvc.perform(get(PANTALLA)).andExpect(redirectedUrl("/publico-ingresar"));
    mvc.perform(get(PROBLEMA)).andExpect(redirectedUrl("/publico-ingresar"));
  }

  @Test
  void muestraLasEntregasConSuUltimaNoticia() throws Exception {
    mvc.perform(get(PANTALLA).param("entidad", ENTIDAD))
        .andExpect(status().isOk())
        .andExpect(model().attribute("seccion", "Entregas"))
        .andExpect(content().string(containsString("<h1>Entregas</h1>")))
        .andExpect(content().string(containsString("Al 8 de noviembre de 2026, 18:30.")))
        .andExpect(content().string(containsString("No tenés entregas para")))
        .andExpect(content().string(containsString("id=\"entrega-d1\"")))
        .andExpect(content().string(containsString("Silla plegable, 1")))
        .andExpect(content().string(containsString("Última noticia: 8 de noviembre, 18:25.")))
        .andExpect(content().string(containsString("2 entregas por llegar")))
        .andExpect(content().string(containsString("Primero va la que tiene un problema.")))
        .andExpect(content().string(not(containsString("null"))))
        .andExpect(content().string(not(containsString("Calle Ejemplo 450"))));
  }

  @Test
  void muestraLoDeDonacionesSinInventarLoQueNoLlega() throws Exception {
    MockHttpSession sesion = new MockHttpSession();
    mvc.perform(get(PANTALLA).param("entidad", ENTIDAD).session(sesion))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Confirmadas en noviembre")))
        .andExpect(content().string(containsString("Fideos secos, 8 kg")))
        .andExpect(content().string(containsString("De Martina Ruiz.")))
        // HG-03: quién recibió no llega; va la organización, con el día de la confirmación
        .andExpect(content().string(containsString("Recibió: ")))
        .andExpect(content().string(containsString(", el 5 de noviembre.")))
        .andExpect(content().string(containsString("De un donante")))
        .andExpect(content().string(not(containsString("Para tu pedido"))))
        .andExpect(content().string(not(containsString("novedades sin leer"))))
        .andExpect(content().string(not(containsString("Confirmar recepción"))));

    // La sesión recuerda la entidad, y el filtro deja solo ese grupo.
    mvc.perform(get(PANTALLA).param("estado", "en-camino").session(sesion))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Mostrando: En camino (1).")))
        .andExpect(content().string(not(containsString("Fideos secos"))))
        .andExpect(content().string(not(containsString("Aceite de girasol"))));
  }

  @Test
  void laEntregaConProblemaLlevaASuPaginaYDiceQueNoHayFechaDeNuevaSalida() throws Exception {
    mvc.perform(get(PANTALLA).param("entidad", ENTIDAD))
        .andExpect(
            content().string(containsString("href=\"/entidad-entrega-problema?entrega=p1\"")))
        // HG-01: la nueva salida no llega del backend
        .andExpect(content().string(containsString("Todavía sin fecha de nueva salida.")));
  }

  @Test
  void laEntregaConProblemaTieneSuDetalleDentroDeEntregas() throws Exception {
    mvc.perform(get(PROBLEMA).param("entidad", ENTIDAD).param("entrega", "p1"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("seccion", "Entregas"))
        .andExpect(
            content().string(containsString("href=\"/entidad-seguimiento\" aria-current=\"page\"")))
        .andExpect(content().string(containsString("aria-label=\"Dónde estás\"")))
        .andExpect(content().string(containsString("<h1>Aceite de girasol, 20 litros</h1>")))
        .andExpect(content().string(containsString("data-estado=\"problema\"")))
        .andExpect(content().string(containsString("El camión no pudo llegar el 7 de noviembre.")))
        .andExpect(content().string(containsString("<dt>Sale de nuevo</dt>")))
        .andExpect(content().string(containsString("<dd>Todavía sin fecha de nueva salida</dd>")))
        .andExpect(content().string(containsString("<dd>7 de noviembre, 16:40.</dd>")))
        .andExpect(content().string(containsString("data-paso=\"problema\"")))
        .andExpect(content().string(containsString("data-paso=\"falta\"")))
        .andExpect(content().string(containsString("href=\"/entidad-necesidades?necesidad=n1\"")))
        .andExpect(content().string(not(containsString("data-papel=\"protagonista\""))))
        .andExpect(content().string(not(containsString("null"))));
    // Sin ?entrega=, la primera con problema
    mvc.perform(get(PROBLEMA).param("entidad", ENTIDAD))
        .andExpect(content().string(containsString("<h1>Aceite de girasol, 20 litros</h1>")));
  }

  @Test
  void unaEntregaQueNoTieneProblemaNoSeInventa() throws Exception {
    mvc.perform(get(PROBLEMA).param("entidad", ENTIDAD).param("entrega", "d1"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("No encontramos esa entrega con problema.")))
        .andExpect(content().string(not(containsString("Silla plegable"))))
        .andExpect(content().string(not(containsString("Sale de nuevo"))));
  }

  @Test
  void sinNadaAsignadoDiceQueNoHayEntregas() throws Exception {
    mvc.perform(get(PANTALLA).param("entidad", VACIA.toString()))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(containsString("Todavía no se asignaron donaciones a tu organización.")))
        .andExpect(content().string(not(containsString("role=\"search\""))));
  }
}
