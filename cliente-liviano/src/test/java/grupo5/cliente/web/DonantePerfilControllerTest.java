package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.MaquetaConfig;
import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.DonantePerfilPuerto;
import grupo5.cliente.puertos.DonantePerfilPuerto.Tipo;
import grupo5.cliente.vistas.DonantePerfilVista;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * En la maqueta, con un puerto que da otra cuenta: la pantalla lee del modelo y elige la versión
 * por el tipo de la cuenta (R11-06).
 */
@WebMvcTest(controllers = {DonantePerfilController.class, PantallasController.class})
@ActiveProfiles("maqueta")
@Import({MaquetaConfig.class, Pantallas.class})
class DonantePerfilControllerTest {
  private static final UUID DONANTE = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000aa");

  private static final DonantePerfilVista PERSONA =
      new DonantePerfilVista(
          "Martina Ruiz",
          "Persona donante · martina@ejemplo.test",
          "Donante Colaborador",
          true,
          null,
          null,
          "Calle Falsa",
          null,
          null,
          null,
          "Caballito",
          "Ciudad de Buenos Aires",
          "martina@ejemplo.test",
          null,
          null,
          "Aceptaste la versión 2 el 3 de noviembre de 2026.",
          "Martina",
          "Ruiz",
          "DNI 12.345.678");

  private static final DonantePerfilVista EMPRESA =
      new DonantePerfilVista(
          "Distribuidora Ejemplo SA",
          "Empresa donante · contacto@ejemplo.test",
          "Empresa donante · Sostenedor",
          false,
          "Alimentos",
          "30-00000000-0",
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          "Aceptaste la versión 2 el 3 de noviembre de 2026.",
          null,
          null,
          null);

  @MockitoBean private DonantePerfilPuerto perfiles;
  @MockitoBean private CuentaActual cuentas;
  @Autowired private MockMvc mvc;

  @Test
  void laPersonaVeSusDatosYNuncaLosDeUnaEmpresa() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(perfiles.tipo(DONANTE)).thenReturn(Tipo.PERSONA);
    when(perfiles.perfil(DONANTE)).thenReturn(PERSONA);

    mvc.perform(get("/donante-perfil"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Persona donante · martina@ejemplo.test")))
        .andExpect(content().string(containsString("Donó: Donante Colaborador")))
        .andExpect(content().string(containsString("value=\"no\" checked")))
        .andExpect(content().string(containsString("value=\"Martina\"")))
        .andExpect(content().string(containsString("value=\"Ruiz\"")))
        .andExpect(content().string(containsString("value=\"DNI 12.345.678\"")))
        .andExpect(content().string(containsString("value=\"Calle Falsa\"")))
        .andExpect(content().string(containsString("Aceptaste la versión 2")))
        .andExpect(content().string(containsString("hx-indicator=\"#enviando-donante-perfil\"")))
        .andExpect(content().string(containsString("href=\"/donante-misiones\"")))
        .andExpect(content().string(not(containsString("Razón social"))))
        .andExpect(content().string(not(containsString("CUIT"))))
        .andExpect(content().string(not(containsString(">Empresa<"))))
        .andExpect(content().string(not(containsString("Empresa donante ·"))))
        .andExpect(content().string(not(containsString("Fecha de nacimiento"))));
  }

  @Test
  void unaCuentaSinTipoNoSeTrataComoEmpresa() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(perfiles.tipo(DONANTE)).thenReturn(Tipo.SIN_TIPO);
    when(perfiles.perfil(DONANTE)).thenReturn(PERSONA);

    mvc.perform(get("/donante-perfil"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("value=\"Martina\"")))
        .andExpect(content().string(not(containsString("Razón social"))))
        .andExpect(content().string(not(containsString("CUIT"))))
        .andExpect(content().string(not(containsString("href=\"/donante-misiones\""))))
        .andExpect(content().string(not(containsString("tus misiones"))));
  }

  @Test
  void laEmpresaSigueConRazonSocialYCuit() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(perfiles.tipo(DONANTE)).thenReturn(Tipo.EMPRESA);
    when(perfiles.perfil(DONANTE)).thenReturn(EMPRESA);

    mvc.perform(get("/donante-perfil"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Razón social")))
        .andExpect(content().string(containsString("value=\"30-00000000-0\"")))
        .andExpect(content().string(not(containsString("href=\"/donante-misiones\""))));
  }

  @Test
  void elTipoSePideUnaVezPorSesion() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(perfiles.tipo(DONANTE)).thenReturn(Tipo.PERSONA);
    when(perfiles.perfil(DONANTE)).thenReturn(PERSONA);
    MockHttpSession sesion = new MockHttpSession();

    mvc.perform(get("/donante-perfil").session(sesion)).andExpect(status().isOk());
    mvc.perform(get("/donante-perfil").session(sesion)).andExpect(status().isOk());

    verify(perfiles, times(1)).tipo(DONANTE);
  }

  @Test
  void sinCuentaMandaAIngresar() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.empty());

    mvc.perform(get("/donante-perfil")).andExpect(redirectedUrl("/publico-ingresar"));
  }
}
