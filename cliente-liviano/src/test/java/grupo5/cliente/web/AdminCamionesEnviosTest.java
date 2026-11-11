package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.puertos.AdminOperacionPuerto;
import grupo5.cliente.puertos.CuentasDelMenu;
import grupo5.cliente.vistas.AdminCamionesVista;
import grupo5.cliente.vistas.AdminCamionesVista.Camion;
import grupo5.cliente.vistas.CuentaVista;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Camiones fuera de la maqueta: la lista con lo que da Logística (sin inventar chofer, viaje ni
 * rendimiento), los envíos que guardan y vuelven con el aviso, y las pantallas de resultado de la
 * maqueta, que acá no existen. El acceso por área lo prueba SeguridadTest.
 */
@WebMvcTest({AdminCamionesController.class, AdminCamionesEnvios.class})
@AutoConfigureMockMvc(addFilters = false)
@Import({Pantallas.class, AdminCamionesEnviosTest.Cuenta.class})
class AdminCamionesEnviosTest {

  private static final String ID = "6f1c2c1e-0000-4000-8000-0000000000c1";

  @TestConfiguration
  static class Cuenta {

    @Bean
    CuentasDelMenu cuentas() {
      return prefijo -> Optional.of(new CuentaVista("Leo Ejemplo", "Administración"));
    }

    /** Sin Keycloak: el ingreso no se usa (sin filtros), pero la configuración lo pide. */
    @Bean
    ClientRegistrationRepository clientes() {
      return new InMemoryClientRegistrationRepository(
          ClientRegistration.withRegistrationId("keycloak")
              .clientId("cliente-liviano")
              .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
              .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
              .authorizationUri("http://keycloak/auth")
              .tokenUri("http://keycloak/token")
              .build());
    }
  }

  @MockitoBean private AdminOperacionPuerto operacion;
  @Autowired private MockMvc mvc;

  @Test
  void laListaMuestraLoQueDaLogisticaSinInventarElResto() throws Exception {
    when(operacion.camiones(""))
        .thenReturn(
            new AdminCamionesVista(
                List.of(
                    new Camion(
                        ID,
                        "AB 123 CD",
                        null,
                        "camion",
                        "3.500 kg · 18 m³",
                        3500,
                        "grande",
                        "ruta",
                        "En ruta",
                        null,
                        null,
                        null,
                        null,
                        null,
                        false)),
                "Al 10 de noviembre de 2026, 18:30.",
                null,
                null));

    mvc.perform(get("/admin-camiones"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("AB 123 CD")))
        .andExpect(content().string(containsString("Al 10 de noviembre de 2026, 18:30.")))
        .andExpect(content().string(not(containsString("Chofer"))))
        .andExpect(content().string(not(containsString("Rendimiento"))))
        .andExpect(content().string(not(containsString("Entregas del"))))
        .andExpect(content().string(not(containsString("Todavía no hizo rutas"))));
  }

  @Test
  void agregarGuardaYVuelveALaFichaConElAviso() throws Exception {
    when(operacion.agregarCamion("AF 345 KL", 2000f, 12f, 2.8f)).thenReturn(ID);

    mvc.perform(
            post("/admin-camiones-agregado")
                .param("patente", " AF 345 KL ")
                .param("capacidadKG", "2000")
                .param("capacidadVolumen", "12")
                .param("altura", "2.8"))
        .andExpect(redirectedUrl("/admin-camiones?camion=" + ID))
        .andExpect(
            flash()
                .attribute(
                    Envios.AVISO,
                    "Agregaste el camión AF 345 KL. Queda disponible para las próximas rutas."));
  }

  @Test
  void deshabilitarYHabilitarCambianElEstadoEnLogistica() throws Exception {
    when(operacion.cambiarCamion(ID, false, "Service")).thenReturn("AB 123 CD");
    when(operacion.cambiarCamion(ID, true, null)).thenReturn("AB 123 CD");

    mvc.perform(
            post("/admin-camiones-deshabilitado").param("camion", ID).param("motivo", "Service"))
        .andExpect(redirectedUrl("/admin-camiones?camion=" + ID))
        .andExpect(
            flash()
                .attribute(
                    Envios.AVISO,
                    "Deshabilitaste el camión AB 123 CD. No se le van a asignar rutas hasta que lo"
                        + " vuelvas a habilitar."));
    mvc.perform(post("/admin-camiones-habilitado").param("camion", ID))
        .andExpect(
            flash()
                .attribute(
                    Envios.AVISO,
                    "Volviste a habilitar el camión AB 123 CD. Queda disponible para las próximas"
                        + " rutas."));
    verify(operacion).cambiarCamion(ID, true, null);
  }

  @Test
  void lasPantallasDeResultadoDeLaMaquetaNoExisten() throws Exception {
    mvc.perform(get("/admin-camiones-agregado")).andExpect(status().isNotFound());
  }
}
