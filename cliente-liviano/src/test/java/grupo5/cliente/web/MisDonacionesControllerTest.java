package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.MaquetaConfig;
import grupo5.cliente.puertos.DonacionesPuerto;
import grupo5.cliente.vistas.DetalleDonacionVista;
import grupo5.cliente.vistas.EstadoEntrega;
import grupo5.cliente.vistas.MisDonacionesVista;
import grupo5.cliente.vistas.MisDonacionesVista.Categoria;
import grupo5.cliente.vistas.MisDonacionesVista.Donacion;
import java.util.List;
import java.util.Optional;
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
 * La pantalla con lo que da Donaciones: categorías por id, una sin ícono, y la cuenta por {@code
 * ?donante=}, que la sesión recuerda.
 */
@WebMvcTest(
    controllers = {MisDonacionesController.class, PantallasController.class},
    properties = "cliente.datos.cuenta=parametro")
@ActiveProfiles("maqueta")
@Import({
  MaquetaConfig.class,
  Pantallas.class,
  CuentaPorParametro.class,
  MisDonacionesControllerTest.DosDonaciones.class
})
class MisDonacionesControllerTest {

  private static final String ALIMENTOS = "6f1c2c1e-0000-4000-8000-0000000000c1";
  private static final String MUEBLES = "6f1c2c1e-0000-4000-8000-0000000000c2";
  private static final String MOBILIARIO = "Mobiliario";
  private static final String DONANTE = "6f1c2c1e-0000-4000-8000-0000000000aa";

  @TestConfiguration
  static class DosDonaciones {

    @Bean
    DonacionesPuerto donaciones() {
      return new DonacionesPuerto() {
        @Override
        public MisDonacionesVista misDonaciones(UUID donante) {
          return new MisDonacionesVista(
              List.of(
                  new Donacion(
                      "b",
                      2,
                      "Silla plegable",
                      "1 unidad",
                      MUEBLES,
                      null,
                      MOBILIARIO,
                      null,
                      "14 de octubre de 2026",
                      EstadoEntrega.PENDIENTE,
                      "En el depósito, espera destino"),
                  new Donacion(
                      "a",
                      1,
                      "Fideos secos",
                      "8 kg",
                      ALIMENTOS,
                      "cat-alimentos",
                      "Alimentos",
                      "Comedor Barrio Ejemplo",
                      "1 de octubre de 2026",
                      EstadoEntrega.ENTREGADA,
                      "Entrega confirmada el 5 de octubre")),
              List.of(new Categoria(ALIMENTOS, "Alimentos"), new Categoria(MUEBLES, MOBILIARIO)),
              "hoy, 7 de noviembre, 19:40");
        }

        @Override
        public Optional<DetalleDonacionVista> detalle(UUID donante, String clave) {
          // Como lo arma el adaptador REST: entregada sin foto y sin "Figura como" (HB-20).
          return "a".equals(clave)
              ? Optional.of(
                  new DetalleDonacionVista(
                      misDonaciones(donante).donaciones().get(1),
                      "5 de octubre de 2026",
                      List.of(),
                      null,
                      null,
                      null,
                      "Comedor Barrio Ejemplo confirmó la entrega el 5 de octubre.",
                      List.of(),
                      null))
              : Optional.empty();
        }
      };
    }
  }

  @Autowired private MockMvc mvc;

  @Test
  void sinCuentaOConUnIdMalEscritoMandaAIngresar() throws Exception {
    mvc.perform(get("/donante-donaciones")).andExpect(redirectedUrl("/publico-ingresar"));
    mvc.perform(get("/donante-donaciones?donante=no-es-un-id"))
        .andExpect(redirectedUrl("/publico-ingresar"));
  }

  @Test
  void laSesionRecuerdaAlDonanteYElFiltroUsaLasCategoriasDeLaApi() throws Exception {
    MockHttpSession sesion = new MockHttpSession();
    mvc.perform(get("/donante-donaciones?donante=" + DONANTE).session(sesion))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    containsString(
                        "2 donaciones, de la más reciente a la primera. Actualizado hoy, 7 de"
                            + " noviembre, 19:40.")))
        .andExpect(content().string(containsString(MOBILIARIO)));

    mvc.perform(get("/donante-donaciones?categoria=" + ALIMENTOS).session(sesion))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("1 donación con esos filtros.")))
        .andExpect(content().string(containsString("Fideos secos")))
        .andExpect(content().string(not(containsString("Silla plegable"))));
  }

  @Test
  void conUnMesMuestraSoloLasDeEseMesYLoDice() throws Exception {
    MockHttpSession sesion = new MockHttpSession();
    mvc.perform(get("/donante-donaciones?mes=2026-10&donante=" + DONANTE).session(sesion))
        .andExpect(status().isOk())
        .andExpect(
            content().string(containsString("Estás viendo las registradas en octubre de 2026.")))
        .andExpect(content().string(containsString("Ver todas tus donaciones")))
        .andExpect(content().string(containsString("name=\"mes\" value=\"2026-10\"")))
        .andExpect(content().string(containsString("Silla plegable")));

    mvc.perform(get("/donante-donaciones?mes=2026-09").session(sesion))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(containsString("No hay donaciones registradas en septiembre de 2026.")))
        .andExpect(content().string(not(containsString("Silla plegable"))));

    mvc.perform(get("/donante-donaciones?mes=no-es-un-mes").session(sesion))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString("Estás viendo las registradas"))))
        .andExpect(content().string(containsString("Silla plegable")));
  }

  @Test
  void elDetalleMuestraLoQueDaLaApiYNoInventaLaDeOtroDonante() throws Exception {
    mvc.perform(get("/donante-donaciones-detalle?donacion=a"))
        .andExpect(redirectedUrl("/publico-ingresar"));

    MockHttpSession sesion = new MockHttpSession();
    mvc.perform(get("/donante-donaciones-detalle?donacion=a&donante=" + DONANTE).session(sesion))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    containsString("Comedor Barrio Ejemplo confirmó la entrega el 5 de octubre.")))
        .andExpect(content().string(not(containsString("Figura como"))))
        .andExpect(content().string(not(containsString("Falta confirmar la entrega"))));

    mvc.perform(get("/donante-donaciones-detalle?donacion=b").session(sesion))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("No encontramos esa donación.")));
  }
}
