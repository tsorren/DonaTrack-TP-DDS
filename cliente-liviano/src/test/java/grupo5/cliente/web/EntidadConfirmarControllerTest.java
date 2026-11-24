package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.MaquetaConfig;
import grupo5.cliente.puertos.EntidadConfirmarPuerto;
import grupo5.cliente.puertos.RecibidasPuerto;
import grupo5.cliente.vistas.EntidadEntregaVista;
import grupo5.cliente.vistas.RecibidasVista;
import grupo5.cliente.vistas.RecibidasVista.Recibida;
import java.util.List;
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
 * Confirmar recepción con lo que dan Logística y Donaciones: una entrega sin remito, sin lo
 * recibido de la necesidad y de quien ocultó su nombre; la entidad llega por {@code ?entidad=}.
 */
@WebMvcTest(
    controllers = {EntidadConfirmarController.class, PantallasController.class},
    properties = "cliente.datos.cuenta=parametro")
@ActiveProfiles("maqueta")
@Import({
  MaquetaConfig.class,
  Pantallas.class,
  CuentaPorParametro.class,
  EntidadConfirmarControllerTest.UnaEntrega.class
})
class EntidadConfirmarControllerTest {

  private static final String ENTREGA = "6f1c2c1e-0000-4000-8000-0000000000a1";

  @TestConfiguration
  static class UnaEntrega {

    /** Dos recibidas en noviembre y una en camino: los frutos son las dos recibidas. */
    @Bean
    RecibidasPuerto recibidas() {
      return entidad ->
          new RecibidasVista(
              List.of(
                  recibida("a", "camino"), recibida("b", "recibida"), recibida("c", "recibida")),
              "noviembre",
              "8 de noviembre de 2026, 18:50",
              null);
    }

    private static Recibida recibida(String clave, String grupo) {
      return new Recibida(
          clave,
          "Silla plegable",
          null,
          null,
          "1",
          "un donante",
          null,
          null,
          grupo,
          null,
          null,
          null,
          null,
          null);
    }

    @Bean
    EntidadConfirmarPuerto confirmar() {
      return new EntidadConfirmarPuerto() {
        @Override
        public List<EntidadEntregaVista> entregas(UUID entidad, String pedida) {
          return List.of(
              new EntidadEntregaVista(
                  ENTREGA,
                  "silla plegable",
                  "Silla plegable, 1 unidad",
                  "1 unidad",
                  "8 de noviembre, 14:30",
                  null,
                  "Salió del depósito el 8 de noviembre a las 14:30.",
                  "Salió del depósito el 8 de noviembre a las 14:30.",
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  1,
                  "unidades",
                  false,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null));
        }

        /** La silla ya confirmada, con lo que Logística guardó de ella. */
        @Override
        public List<EntidadEntregaVista> confirmada(UUID entidad, String pedida, String motivo) {
          return List.of(
              new EntidadEntregaVista(
                  ENTREGA,
                  "silla plegable",
                  "Silla plegable, 1 unidad",
                  "1 unidad",
                  "8 de noviembre, 14:30",
                  "8 de noviembre, 18:20",
                  "Llegó a tu puerta el 8 de noviembre a las 18:20.",
                  "Llegó a tu puerta el 8 de noviembre a las 18:20.",
                  "0001-00009999",
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  "https://fotos.ejemplo/1.jpg",
                  "8 de noviembre de 2026, 18:45",
                  1,
                  "unidades",
                  false,
                  null,
                  null,
                  "6 de noviembre, 10:00",
                  "7 de noviembre, 11:00",
                  null,
                  null));
        }

        @Override
        public String hoy() {
          return "8 de noviembre de 2026";
        }

        @Override
        public void confirmar(UUID entidad, String clave, Acta acta) {
          // No hace falta en este test.
        }
      };
    }
  }

  @Autowired private MockMvc mvc;

  @Test
  void sinEntidadMandaAIngresar() throws Exception {
    mvc.perform(get("/entidad-seguimiento-confirmar"))
        .andExpect(redirectedUrl("/publico-ingresar"));
  }

  @Test
  void elActaDeUnaEntregaDeLasApis() throws Exception {
    MockHttpSession sesion = new MockHttpSession();
    mvc.perform(
            get("/entidad-seguimiento-confirmar")
                .param("entidad", "6f1c2c1e-0000-4000-8000-0000000000e1")
                .param("entrega", ENTREGA)
                .session(sesion))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    containsString(
                        "<strong>Silla plegable, 1 unidad</strong>, de un donante. Salió del"
                            + " depósito el 8 de")))
        .andExpect(
            content().string(containsString("action=\"/entidad-seguimiento-sellada-" + ENTREGA)))
        .andExpect(
            content()
                .string(containsString("hx-indicator=\"#enviando-entidad-seguimiento-confirmar\"")))
        .andExpect(content().string(containsString(", el 8 de noviembre de 2026</p>")))
        .andExpect(content().string(containsString(", de un donante.")))
        .andExpect(content().string(not(containsString("null"))))
        .andExpect(content().string(not(containsString("15 de octubre"))));

    mvc.perform(get("/entidad-seguimiento-problema").param("entrega", ENTREGA).session(sesion))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Cuánto llegó, en unidades")));

    mvc.perform(get("/entidad-seguimiento-confirmar").param("entrega", "otra").session(sesion))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("No encontramos esa entrega")));
  }

  /**
   * ORG-11 y ORG-12: Recepción confirmada con lo que asentó Logística (el remito, la foto y
   * cuándo); quién recibió es la organización (HG-03), sin un dato de la maqueta ni de la guía.
   */
  @Test
  void laRecepcionConfirmadaDeUnaEntregaDeLasApis() throws Exception {
    mvc.perform(
            get("/entidad-seguimiento-sellada")
                .param("entidad", "6f1c2c1e-0000-4000-8000-0000000000e1")
                .param("entrega", ENTREGA))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    containsString(
                        "Confirmaste la recepción de <strong>Silla plegable, 1 unidad</strong>.")))
        .andExpect(content().string(containsString("<dd>0001-00009999</dd>")))
        .andExpect(content().string(containsString("<dd>8 de noviembre de 2026, 18:45</dd>")))
        .andExpect(content().string(containsString("src=\"https://fotos.ejemplo/1.jpg\"")))
        .andExpect(content().string(containsString("Recibió: ")))
        .andExpect(content().string(containsString(", el 8 de noviembre.")))
        // D4: los frutos del mes, en el dibujo y con su cifra; D2: cada paso con su fecha
        .andExpect(content().string(containsString("data-dibujo=\"frutos\"")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "2 entregas confirmadas</strong> por tu organización en noviembre.")))
        .andExpect(content().string(containsString(">6 de noviembre, 10:00</span>")))
        .andExpect(content().string(containsString(">7 de noviembre, 11:00</span>")))
        .andExpect(content().string(containsString(">8 de noviembre, 14:30</span>")))
        .andExpect(content().string(containsString("No te quedan entregas para confirmar.")))
        .andExpect(content().string(not(containsString("Foto de ejemplo"))))
        .andExpect(content().string(not(containsString("huerta-raiz-trazo"))))
        .andExpect(content().string(not(containsString("Marta"))))
        .andExpect(content().string(not(containsString("15 de octubre"))))
        .andExpect(content().string(not(containsString("null"))));
  }

  /**
   * U3: en la respuesta al acta, la recién confirmada es el fruto que brota, aunque Donaciones
   * todavía no la tenga entre las recibidas del mes (las dos del puerto son otras).
   */
  @Test
  void laRespuestaAlActaCuentaLaRecienConfirmadaEntreLosFrutos() throws Exception {
    mvc.perform(
            get("/entidad-seguimiento-sellada")
                .param("entidad", "6f1c2c1e-0000-4000-8000-0000000000e1")
                .param("entrega", ENTREGA)
                .flashAttr("recien", true))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    containsString(
                        "3 entregas confirmadas</strong> por tu organización en noviembre.")))
        .andExpect(content().string(containsString("data-recien=\"si\"")))
        .andExpect(content().string(containsString("huerta-raiz-trazo")));
  }
}
