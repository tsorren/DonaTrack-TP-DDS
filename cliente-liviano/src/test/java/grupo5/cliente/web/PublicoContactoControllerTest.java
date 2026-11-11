package grupo5.cliente.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.puertos.CuentasDelMenu;
import grupo5.cliente.puertos.PublicoContactoPuerto;
import grupo5.cliente.puertos.PublicoContactoPuerto.Mensaje;
import grupo5.cliente.puertos.PublicoContactoPuerto.Motivo;
import grupo5.cliente.puertos.PublicoContactoPuerto.Rechazado;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contacto fuera de la maqueta: el formulario lleva la clave del puerto, el envío guarda y lleva al
 * enviado con su número, y el límite vuelve al formulario con su aviso. La seguridad se prueba
 * aparte (SeguridadTest: las públicas no piden ingreso).
 */
@WebMvcTest({PublicoContactoController.class, PublicoContactoEnvioController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(Pantallas.class)
class PublicoContactoControllerTest {

  private static final UUID CLAVE = UUID.fromString("00000000-0000-4000-8000-000000000021");

  @MockitoBean private PublicoContactoPuerto contacto;
  @MockitoBean private CuentasDelMenu cuentas;
  @MockitoBean private ClientRegistrationRepository registros;
  @Autowired private MockMvc mvc;

  @Test
  void elFormularioLlevaLaClaveDeEnvioOculta() throws Exception {
    when(contacto.nuevaClave()).thenReturn(CLAVE);

    mvc.perform(get("/publico-contacto"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    containsString(
                        "<input type=\"hidden\" name=\"claveEnvio\" value=\"" + CLAVE + "\">")))
        .andExpect(content().string(containsString("hx-indicator=\"#enviando-publico-contacto\"")))
        .andExpect(content().string(containsString("name=\"sitio\"")));
  }

  @Test
  void enviarGuardaYLlevaAlEnviadoConElNumeroYElAviso() throws Exception {
    when(contacto.enviar(any(), any())).thenReturn(77L);

    mvc.perform(
            post("/publico-contacto-enviado")
                .param("volver", "/entidad-necesidades")
                .param("tema", "Algo del sitio no anduvo")
                .param("mensaje", "No cargó el mapa.")
                .param("codigo", " ")
                .param("correo", "ana@ejemplo.test")
                .param("claveEnvio", CLAVE.toString())
                .with(
                    pedido -> {
                      pedido.setRemoteAddr("203.0.113.7");
                      return pedido;
                    }))
        .andExpect(redirectedUrl("/publico-contacto-enviado?volver=%2Fentidad-necesidades"))
        .andExpect(flash().attribute("numeroDelMensaje", 77L))
        .andExpect(flash().attribute(Envios.AVISO, "Recibimos tu mensaje."));

    ArgumentCaptor<Mensaje> mensaje = ArgumentCaptor.forClass(Mensaje.class);
    verify(contacto).enviar(mensaje.capture(), eq("203.0.113.7"));
    assertThat(mensaje.getValue())
        .isEqualTo(new Mensaje("SITIO", "No cargó el mapa.", null, "ana@ejemplo.test", CLAVE));
  }

  @Test
  void elEnviadoMuestraElNumeroYSinEnvioVuelveAlFormulario() throws Exception {
    when(contacto.numeroDeEjemplo()).thenReturn(null);

    mvc.perform(get("/publico-contacto-enviado").flashAttr("numeroDelMensaje", 77L))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Su número es <b>77</b>")));

    mvc.perform(get("/publico-contacto-enviado")).andExpect(redirectedUrl("/publico-contacto"));
  }

  @Test
  void conElLimiteVuelveAlFormularioConSuAviso() throws Exception {
    when(contacto.enviar(any(), any())).thenThrow(new Rechazado(Motivo.LIMITE, null));

    mvc.perform(
            post("/publico-contacto-enviado")
                .param("tema", "Otra cosa")
                .param("mensaje", "Hola")
                .param("correo", "ana@ejemplo.test"))
        .andExpect(redirectedUrl("/publico-contacto"))
        .andExpect(
            flash()
                .attribute(
                    Envios.AVISO,
                    "No pudimos enviar este mensaje: ya mandaste varios seguidos. Los anteriores"
                        + " nos llegaron; probá de nuevo más tarde."));
  }

  @Test
  void sinTemaVaComoOtroYSinClaveLlevaUnaNueva() throws Exception {
    when(contacto.enviar(any(), any())).thenReturn(5L);

    mvc.perform(
            post("/publico-contacto-enviado")
                .param("mensaje", "Hola")
                .param("correo", "ana@ejemplo.test"))
        .andExpect(redirectedUrl("/publico-contacto-enviado"));

    ArgumentCaptor<Mensaje> mensaje = ArgumentCaptor.forClass(Mensaje.class);
    verify(contacto).enviar(mensaje.capture(), any());
    assertThat(mensaje.getValue().tema()).isEqualTo("OTRO");
    assertThat(mensaje.getValue().claveEnvio()).isNotNull();
  }

  @Test
  void conElCampoTrampaEscritoMuestraElAvisoSinMandarNada() throws Exception {
    mvc.perform(
            post("/publico-contacto-enviado")
                .param("mensaje", "Hola")
                .param("correo", "ana@ejemplo.test")
                .param("sitio", "https://spam.test"))
        .andExpect(redirectedUrl("/publico-contacto"))
        .andExpect(flash().attribute(Envios.AVISO, "Recibimos tu mensaje."));

    verify(contacto, never()).enviar(any(), any());
  }
}
