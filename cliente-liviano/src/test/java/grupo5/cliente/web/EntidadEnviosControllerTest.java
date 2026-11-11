package grupo5.cliente.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.EntidadNotificacionesPuerto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Los envíos reales de la entidad (fuera de la maqueta): llaman al puerto, redirigen a la pantalla
 * y dejan el aviso. La cadena de seguridad la prueba SeguridadTest; acá va sin filtros.
 */
@WebMvcTest(EntidadEnviosController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "CLIENTE_LIVIANO_SECRET=prueba")
class EntidadEnviosControllerTest {

  private static final UUID ENTIDAD = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000e1");
  private static final UUID UNA = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000a1");
  private static final String LEIDAS = "/entidad-notificaciones-leidas";
  private static final String BANDEJA = "/entidad-notificaciones";

  @MockitoBean private ClientRegistrationRepository clientes;
  @MockitoBean private CuentaActual cuentas;
  @MockitoBean private EntidadNotificacionesPuerto notificaciones;
  @Autowired private MockMvc mvc;

  @BeforeEach
  void conEntidad() {
    when(cuentas.entidad(any())).thenReturn(Optional.of(ENTIDAD));
  }

  @Test
  void marcarUnaTodasODejarSinLeerVuelveALaBandejaConSuAviso() throws Exception {
    mvc.perform(post(LEIDAS).param("marcar", UNA.toString()))
        .andExpect(redirectedUrl(BANDEJA))
        .andExpect(flash().attribute(Envios.AVISO, "Marcaste las novedades como leídas."));
    verify(notificaciones).marcarLeidas(ENTIDAD, List.of(UNA));

    mvc.perform(post(LEIDAS)).andExpect(redirectedUrl(BANDEJA));
    verify(notificaciones).marcarLeidas(ENTIDAD, List.of());

    mvc.perform(post(LEIDAS).param("no-leer", UNA.toString()))
        .andExpect(redirectedUrl(BANDEJA))
        .andExpect(flash().attribute(Envios.AVISO, "Dejaste la novedad sin leer."));
    verify(notificaciones).dejarSinLeer(UNA);
  }

  @Test
  void elPerfilTodaviaNoGuardaYLoDice() throws Exception {
    mvc.perform(post("/entidad-perfil-guardado").param("seccion", "avisos"))
        .andExpect(redirectedUrl("/entidad-perfil#como-te-avisamos"))
        .andExpect(
            flash()
                .attribute(
                    Envios.AVISO,
                    "Todavía no podemos guardar estos cambios desde acá: siguen como estaban. Si"
                        + " necesitás cambiar algo, escribinos desde Contacto."));
    mvc.perform(post("/entidad-perfil-guardado").param("seccion", "datos"))
        .andExpect(redirectedUrl("/entidad-perfil#datos"));
  }

  @Test
  void sinEntidadMandaAIngresar() throws Exception {
    when(cuentas.entidad(any())).thenReturn(Optional.empty());
    mvc.perform(post(LEIDAS)).andExpect(redirectedUrl("/publico-ingresar"));
  }
}
