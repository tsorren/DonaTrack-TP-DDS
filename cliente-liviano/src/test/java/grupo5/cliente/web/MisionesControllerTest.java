package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.MaquetaConfig;
import grupo5.cliente.puertos.MisionesPuerto;
import grupo5.cliente.vistas.MisionesVista;
import grupo5.cliente.vistas.MisionesVista.Categoria;
import grupo5.cliente.vistas.MisionesVista.Insignia;
import grupo5.cliente.vistas.MisionesVista.Mision;
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
import org.springframework.web.client.RestClientException;

/** Pruebas del controlador de misiones e insignias del donante (RF-07). */
@WebMvcTest(
    controllers = {MisionesController.class, PantallasController.class},
    properties = "cliente.datos.cuenta=parametro")
@ActiveProfiles("maqueta")
@Import({
  MaquetaConfig.class,
  Pantallas.class,
  CuentaPorParametro.class,
  MisionesControllerTest.MisionesStub.class
})
class MisionesControllerTest {

  private static final UUID DONANTE_HUMANO =
      UUID.fromString("6f1c2c1e-0000-4000-8000-000000000001");
  private static final UUID DONANTE_EMPRESA =
      UUID.fromString("6f1c2c1e-0000-4000-8000-000000000002");
  private static final UUID DONANTE_NUEVO = UUID.fromString("6f1c2c1e-0000-4000-8000-000000000003");
  private static final UUID DONANTE_ERROR = UUID.fromString("6f1c2c1e-0000-4000-8000-000000000004");

  @TestConfiguration
  static class MisionesStub {

    @Bean
    MisionesPuerto misiones() {
      return donanteId -> {
        if (DONANTE_ERROR.equals(donanteId)) {
          throw new RestClientException("Error simulado de Incentivos");
        }
        if (DONANTE_EMPRESA.equals(donanteId)) {
          return MisionesVista.sinPermiso();
        }
        if (DONANTE_NUEVO.equals(donanteId)) {
          return new MisionesVista(
              true,
              true,
              "Gracias por sumarte, Carlos. Sos donante Colaborador. Todavía no completaste ninguna misión. Al 15 de octubre de 2026, 16:25.",
              List.of(
                  new Mision(
                      "Colaborador",
                      "Racha Inicial",
                      "3 meses seguidos donando",
                      "curso",
                      null,
                      0,
                      3,
                      "0 de 3 meses",
                      null,
                      null,
                      new Insignia("Racha Inicial", "3 meses", "/img/racha.png", true, null))),
              List.of(new Categoria("Colaborador", "tu categoría", "Sostenedor", null)),
              List.of());
        }
        return new MisionesVista(
            true,
            false,
            "Gracias por donar, Martina. Sos donante Sostenedor desde el 2 de septiembre de 2026. Completaste 2 de las 6 misiones. Al 15 de octubre de 2026, 16:25.",
            List.of(
                new Mision(
                    "Colaborador",
                    "Racha Inicial",
                    "3 meses seguidos donando",
                    "completa",
                    "3 de agosto de 2026",
                    3,
                    3,
                    "3 de 3 meses",
                    null,
                    null,
                    new Insignia(
                        "Racha Inicial", "3 meses", "/img/racha.png", true, "3 de agosto de 2026")),
                new Mision(
                    "Colaborador",
                    "Explorador",
                    "Donaciones en 3 categorías distintas",
                    "completa",
                    "2 de septiembre de 2026",
                    3,
                    3,
                    "3 de 3 categorías",
                    null,
                    null,
                    new Insignia(
                        "Explorador",
                        "3 categorías",
                        "/img/explorador.png",
                        true,
                        "2 de septiembre de 2026")),
                new Mision(
                    "Sostenedor",
                    "Gran Aporte",
                    "Una donación con al menos 50 bienes",
                    "curso",
                    null,
                    12,
                    50,
                    "12 de 50 bienes",
                    null,
                    null,
                    new Insignia("Gran Aporte", "50 bienes", "/img/gran-aporte.png", true, null)),
                new Mision(
                    "Sostenedor",
                    "Constancia",
                    "6 meses seguidos donando",
                    "espera",
                    null,
                    0,
                    6,
                    "0 de 6 meses",
                    null,
                    "Gran Aporte",
                    new Insignia("Constancia", "6 meses", "/img/constancia.png", true, null))),
            List.of(
                new Categoria("Colaborador", "completa", null, null),
                new Categoria("Sostenedor", "tu categoría", "Transformador", null)),
            List.of(
                new Mision(
                    "Colaborador",
                    "Racha Inicial",
                    "3 meses seguidos donando",
                    "completa",
                    "3 de agosto de 2026",
                    3,
                    3,
                    "3 de 3 meses",
                    null,
                    null,
                    new Insignia(
                        "Racha Inicial", "3 meses", "/img/racha.png", true, "3 de agosto de 2026")),
                new Mision(
                    "Colaborador",
                    "Explorador",
                    "Donaciones en 3 categorías distintas",
                    "completa",
                    "2 de septiembre de 2026",
                    3,
                    3,
                    "3 de 3 categorías",
                    null,
                    null,
                    new Insignia(
                        "Explorador",
                        "3 categorías",
                        "/img/explorador.png",
                        true,
                        "2 de septiembre de 2026"))));
      };
    }
  }

  @Autowired private MockMvc mvc;

  @Test
  void sinCuentaMandaAIngresar() throws Exception {
    mvc.perform(get("/donante-misiones")).andExpect(redirectedUrl("/publico-ingresar"));
    mvc.perform(get("/donante-misiones?donante=no-es-uuid"))
        .andExpect(redirectedUrl("/publico-ingresar"));
  }

  @Test
  void donanteAutenticadoObtieneSusMisionesEInsignias() throws Exception {
    MockHttpSession sesion = new MockHttpSession();
    mvc.perform(get("/donante-misiones?donante=" + DONANTE_HUMANO).session(sesion))
        .andExpect(status().isOk())
        .andExpect(model().attribute("persona", true))
        .andExpect(
            content()
                .string(
                    containsString(
                        "Gracias por donar, Martina. Sos donante Sostenedor desde el 2 de septiembre de 2026.")))
        .andExpect(content().string(containsString("Gran Aporte")))
        .andExpect(content().string(containsString("12 de 50 bienes")))
        .andExpect(
            content().string(containsString("Empieza a contar cuando completes Gran Aporte.")))
        .andExpect(content().string(containsString("(2 de 6)")));
  }

  @Test
  void ignoraParametrosEnLaUrlParaPrevenirSuplantacionDeDonante() throws Exception {
    MockHttpSession sesion = new MockHttpSession();
    // La sesión tiene al donante humano
    mvc.perform(get("/donante-misiones?donante=" + DONANTE_HUMANO).session(sesion))
        .andExpect(status().isOk());

    // Se intenta suplantar pasando el id de la empresa en la url pero manteniendo la sesión del
    // donante humano
    mvc.perform(get("/donante-misiones?donanteId=" + DONANTE_EMPRESA).session(sesion))
        .andExpect(status().isOk())
        .andExpect(model().attribute("persona", true))
        .andExpect(content().string(containsString("Martina")));
  }

  @Test
  void cuentaQueNoEsPersonaHumanaMuestraMensajeSinPermiso() throws Exception {
    MockHttpSession sesion = new MockHttpSession();
    mvc.perform(get("/donante-misiones?donante=" + DONANTE_EMPRESA).session(sesion))
        .andExpect(status().isOk())
        .andExpect(model().attribute("persona", false))
        .andExpect(
            content()
                .string(containsString("Misiones e insignias es para quienes donan como persona.")))
        .andExpect(content().string(containsString("Las organizaciones donan sin misiones")))
        .andExpect(content().string(not(containsString("Gran Aporte"))));
  }

  @Test
  void listasVaciasDeMisionesMuestraEstadoNuevo() throws Exception {
    MockHttpSession sesion = new MockHttpSession();
    mvc.perform(get("/donante-misiones?donante=" + DONANTE_NUEVO).session(sesion))
        .andExpect(status().isOk())
        .andExpect(model().attribute("persona", true))
        .andExpect(content().string(containsString("Gracias por sumarte, Carlos.")))
        .andExpect(content().string(containsString("Todavía no tenés insignias.")))
        .andExpect(content().string(containsString("0 de 3 meses")));
  }

  @Test
  void fallaDeIncentivosMuestraBannerDeError() throws Exception {
    MockHttpSession sesion = new MockHttpSession();
    mvc.perform(get("/donante-misiones?donante=" + DONANTE_ERROR).session(sesion))
        .andExpect(status().isOk())
        .andExpect(model().attribute("escenario", "error"))
        .andExpect(content().string(containsString("No pudimos cargar tus misiones.")))
        .andExpect(content().string(containsString("a1b2c3d4e5f6")));
  }
}
