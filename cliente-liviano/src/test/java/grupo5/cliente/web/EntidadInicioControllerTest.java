package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.MaquetaConfig;
import grupo5.cliente.puertos.EntidadInicioPuerto;
import grupo5.cliente.puertos.RecibidasPuerto;
import grupo5.cliente.vistas.EntidadInicioVista;
import grupo5.cliente.vistas.EntidadInicioVista.Necesidad;
import grupo5.cliente.vistas.RecibidasVista;
import grupo5.cliente.vistas.RecibidasVista.Recibida;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * El Inicio con lo que dan las APIs: una en camino y una necesidad sin lo recibido; la entidad
 * llega por {@code ?entidad=}.
 */
@WebMvcTest(
    controllers = {EntidadInicioController.class, PantallasController.class},
    properties = "cliente.datos.cuenta=parametro")
@ActiveProfiles("maqueta")
@Import({
  MaquetaConfig.class,
  Pantallas.class,
  CuentaPorParametro.class,
  EntidadInicioControllerTest.DeLasApis.class
})
class EntidadInicioControllerTest {

  private static final String PANTALLA = "/entidad-inicio";
  private static final String ENTIDAD = "6f1c2c1e-0000-4000-8000-0000000000e1";

  @TestConfiguration
  static class DeLasApis {

    @Bean
    RecibidasPuerto recibidas() {
      return entidad ->
          new RecibidasVista(
              List.of(
                  new Recibida(
                      "a",
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
                      "b",
                      "Aceite",
                      null,
                      null,
                      "10 litros",
                      "un donante",
                      null,
                      null,
                      "problema",
                      "El camión no pudo llegar el 7 de noviembre.",
                      null,
                      null,
                      null,
                      "no pudo llegar el 7 de noviembre"),
                  new Recibida(
                      "c",
                      "Fideos secos",
                      null,
                      null,
                      "8 kg",
                      "un donante",
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
    EntidadInicioPuerto inicio() {
      return entidad ->
          new EntidadInicioVista(
              "domingo 8 de noviembre de 2026",
              "8 de noviembre, 18:30",
              List.of(
                  new Necesidad("Sillas", null, null, null, 6, "Hasta el 20 de noviembre"),
                  new Necesidad("Arroz", null, "15 de 40 kg", 15, 40, "Hasta el 30 de noviembre")));
    }
  }

  @Autowired private MockMvc mvc;

  @Test
  void sinEntidadMandaAIngresar() throws Exception {
    mvc.perform(get(PANTALLA)).andExpect(redirectedUrl("/publico-ingresar"));
  }

  @Test
  void muestraLoDeLasApisSinInventarLoQueNoLlega() throws Exception {
    mvc.perform(get(PANTALLA).param("entidad", ENTIDAD))
        .andExpect(status().isOk())
        .andExpect(
            content().string(containsString("Salió del depósito el 8 de noviembre a las 9:15.")))
        .andExpect(content().string(containsString("Sillas")))
        .andExpect(content().string(containsString("Hasta el 20 de noviembre")))
        .andExpect(
            content().string(containsString("2 necesidades vigentes al 8 de noviembre, 18:30.")))
        .andExpect(content().string(not(containsString("null"))))
        .andExpect(content().string(not(containsString("octubre"))));
  }

  /**
   * A09, sin nada en la puerta: la lámina deja de ser la protagonista y dice la última confirmada.
   */
  @Test
  void sinEntregaEnLaPuertaLaLaminaDiceLaUltimaConfirmada() throws Exception {
    mvc.perform(get(PANTALLA).param("entidad", ENTIDAD))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("No tenés entregas para confirmar")))
        .andExpect(content().string(not(containsString("data-papel=\"protagonista\""))))
        .andExpect(content().string(containsString("La última que confirmaste:")))
        .andExpect(content().string(containsString("Fideos secos, 8 kg")))
        .andExpect(content().string(containsString("Entrega confirmada")))
        .andExpect(content().string(containsString("1 entrega confirmada")))
        .andExpect(content().string(containsString("por tu organización en noviembre")))
        .andExpect(content().string(containsString("Al 8 de noviembre, 18:30.")));
  }

  /** La entrega con problema va en un aviso arriba, sin la fecha de la nueva salida (HG-01). */
  @Test
  void laEntregaConProblemaVaEnUnAvisoSinInventarLaNuevaSalida() throws Exception {
    mvc.perform(get(PANTALLA).param("entidad", ENTIDAD))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("class=\"aviso\" data-tipo=\"error\"")))
        .andExpect(content().string(containsString("Aceite, 10 litros: con problema")))
        .andExpect(content().string(containsString("El camión no pudo llegar el 7 de noviembre.")))
        .andExpect(content().string(containsString("Todavía sin fecha de nueva salida.")));
  }

  /** El avance de cada necesidad con la cuenta de hoy (HG-04); sin cuenta, sin barra. */
  @Test
  void elAvanceDeCadaNecesidadUsaLaBarraDelCatalogoConSuCuenta() throws Exception {
    mvc.perform(get(PANTALLA).param("entidad", ENTIDAD))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("15 de 40 kg")))
        .andExpect(content().string(containsString("aria-label=\"Llegaron 15 de 40 kg\"")))
        .andExpect(content().string(containsString("class=\"progreso\"")))
        .andExpect(content().string(not(containsString("Llegaron null"))));
  }
}
