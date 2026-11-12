package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.MaquetaConfig;
import grupo5.cliente.puertos.EntidadNecesidadesPuerto;
import grupo5.cliente.vistas.EntidadNecesidadesVista;
import grupo5.cliente.vistas.EntidadNecesidadesVista.Entrega;
import grupo5.cliente.vistas.EntidadNecesidadesVista.Grupo;
import grupo5.cliente.vistas.EntidadNecesidadesVista.Necesidad;
import grupo5.cliente.vistas.EntidadNecesidadesVista.Opcion;
import grupo5.cliente.vistas.EntidadNecesidadesVista.Rubro;
import grupo5.cliente.vistas.EntidadNecesidadesVista.Terminada;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Necesidades con lo que da Donaciones: una categoría que no es de las cuatro del ejemplo, una sin
 * lo recibido, una entrega con problema y una que se repite sin períodos; la entidad llega por
 * {@code ?entidad=}.
 */
@WebMvcTest(
    controllers = {EntidadNecesidadesController.class, PantallasController.class},
    properties = "cliente.datos.cuenta=parametro")
@ActiveProfiles("maqueta")
@Import({
  MaquetaConfig.class,
  Pantallas.class,
  CuentaPorParametro.class,
  EntidadNecesidadesControllerTest.DeDonaciones.class
})
class EntidadNecesidadesControllerTest {

  private static final String PANTALLA = "/entidad-necesidades";
  private static final String ID = "6f1c2c1e-0000-4000-8000-0000000000b2";
  private static final String CLAVE = "necesidad-" + ID;

  /** La entidad recién aprobada: sin necesidades. */
  private static final UUID VACIA = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000f0");

  @TestConfiguration
  static class DeDonaciones {

    @Bean
    EntidadNecesidadesPuerto necesidades() {
      return new EntidadNecesidadesPuerto() {
        @Override
        public EntidadNecesidadesVista necesidades(UUID entidad, String accion) {
          return new EntidadNecesidadesVista(
              "8 de noviembre de 2026",
              "2026-11-08",
              "8 de noviembre",
              VACIA.equals(entidad)
                  ? List.of()
                  : List.of(
                      new Necesidad(
                          CLAVE,
                          ID,
                          2L,
                          "Bicicletas",
                          null,
                          "Movilidad",
                          "unidades",
                          "Hasta el 20 de noviembre",
                          "faltan 12 días",
                          "Una sola vez",
                          null,
                          6,
                          null,
                          null,
                          "Para ir a la escuela.",
                          0,
                          0,
                          1,
                          List.of(
                              new Entrega(
                                  "1 unidad",
                                  "con-problema",
                                  "El camión no pudo llegar el 7 de noviembre.",
                                  null)),
                          List.of(),
                          null,
                          List.of()),
                      new Necesidad(
                          "necesidad-otra",
                          "6f1c2c1e-0000-4000-8000-0000000000b3",
                          1L,
                          "Leche",
                          null,
                          "Movilidad",
                          "unidades",
                          null,
                          null,
                          "Cada semana",
                          null,
                          10,
                          null,
                          null,
                          "Para la merienda.",
                          // R11-43: 5 en curso y se muestra 1: el resumen lleva "Ver todas"
                          4,
                          1,
                          0,
                          List.of(
                              new Entrega(
                                  "2 unidades", "en-tu-puerta", "Llegaron a tu puerta.", null)),
                          List.of(),
                          null,
                          List.of())),
              List.of(new Rubro("Movilidad", "Movilidad", null)),
              List.of(new Terminada("Frazadas · 20 unidades", "La diste de baja.")),
              List.of(
                  new Grupo(
                      "Movilidad",
                      List.of(
                          new Opcion(
                              "6f1c2c1e-0000-4000-8000-0000000000c9",
                              "Bicicletas (unidades)",
                              false)))),
              null,
              CLAVE);
        }

        @Override
        public void publicar(
            UUID entidad,
            UUID subcategoria,
            int cantidad,
            String periodicidad,
            LocalDate hasta,
            String motivo) {
          // No hace falta en este test.
        }

        @Override
        public void cambiar(UUID necesidad, long version, int cantidad, String motivo) {
          // No hace falta en este test.
        }

        @Override
        public void darDeBaja(UUID necesidad) {
          // No hace falta en este test.
        }
      };
    }
  }

  @Autowired private MockMvc mvc;

  @Test
  void sinNecesidadesInvitaAPublicar() throws Exception {
    mvc.perform(get(PANTALLA).param("entidad", VACIA.toString()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("No tenés necesidades vigentes.")))
        .andExpect(content().string(containsString("Publicar una necesidad")));
  }

  @Test
  void sinEntidadMandaAIngresar() throws Exception {
    mvc.perform(get(PANTALLA)).andExpect(redirectedUrl("/publico-ingresar"));
  }

  @Test
  void muestraLasDeDonacionesConSusFormulariosReales() throws Exception {
    mvc.perform(
            get(PANTALLA)
                .param("entidad", "6f1c2c1e-0000-4000-8000-0000000000e1")
                .param("necesidad", CLAVE))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Al 8 de noviembre de 2026")))
        .andExpect(content().string(containsString("id=\"cantero-Movilidad\"")))
        .andExpect(content().string(containsString("Bicicletas")))
        .andExpect(content().string(containsString("Pide 6 unidades")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "href=\"/entidad-necesidades?necesidad=" + CLAVE + "#" + CLAVE + "\"")))
        .andExpect(content().string(containsString("href=\"/entidad-seguimiento\">Entregas")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "action=\"/entidad-necesidades-cambiada?necesidad="
                            + ID
                            + "&amp;version=2\"")))
        .andExpect(
            content().string(containsString("action=\"/entidad-necesidades-baja?necesidad=" + ID)))
        .andExpect(
            content()
                .string(
                    containsString(
                        "<option value=\"6f1c2c1e-0000-4000-8000-0000000000c9\">Bicicletas (unidades)</option>")))
        .andExpect(content().string(containsString("min=\"2026-11-08\"")))
        .andExpect(content().string(containsString("Desde hoy, 8 de noviembre, en adelante.")))
        .andExpect(content().string(containsString("Cumplidas y terminadas (1)")))
        // Regla 2: el resumen de las entregas en curso lleva "Ver todas", a Recibidas en curso
        .andExpect(content().string(containsString("Mostramos 1 de 5 entregas en curso.")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "href=\"/entidad-recibidas?estado=en-tu-puerta&amp;estado=con-problema"
                            + "&amp;estado=en-camino&amp;estado=por-salir\">Ver")))
        .andExpect(content().string(not(containsString("null"))))
        .andExpect(content().string(not(containsString("15 de octubre"))));
  }
}
