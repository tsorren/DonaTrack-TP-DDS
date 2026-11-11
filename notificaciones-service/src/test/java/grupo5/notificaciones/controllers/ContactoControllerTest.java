package grupo5.notificaciones.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.common.CommonLibAutoConfiguration;
import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.logging.LoggingAutoConfiguration;
import grupo5.notificaciones.dto.contacto.MensajeContactoDTO;
import grupo5.notificaciones.models.entities.contacto.EstadoMensajeContacto;
import grupo5.notificaciones.services.impl.ContactoService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ContactoController.class)
@Import({CommonLibAutoConfiguration.class, LoggingAutoConfiguration.class})
class ContactoControllerTest {

  private static final String CUERPO =
      """
      {"tema": "ENTREGA", "texto": "No llegó la entrega", "codigoError": null,
       "correo": "ana@ejemplo.org", "claveEnvio": "6f1c2b9e-3d4a-4b5c-8d6e-7f8091a2b3c4"}
      """;

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ContactoService service;

  private static MensajeContactoDTO mensaje(long numero, String estado) {
    return new MensajeContactoDTO(
        numero,
        "ENTREGA",
        "No llegó la entrega",
        null,
        "ana@ejemplo.org",
        estado,
        LocalDateTime.of(2026, 11, 10, 9, 0),
        null,
        null);
  }

  @Test
  void recibir_conDatosValidos_responde201ConElNumeroYPasaLaIp() throws Exception {
    when(service.recibir(any(), eq("203.0.113.7")))
        .thenReturn(new ContactoService.Recepcion(4821, true));

    mockMvc
        .perform(
            post("/api/contacto")
                .header("X-Forwarded-For", "203.0.113.7")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CUERPO))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.numero").value(4821));
  }

  @Test
  void recibir_conClaveRepetida_responde200ConElNumeroDelPrimero() throws Exception {
    when(service.recibir(any(), any())).thenReturn(new ContactoService.Recepcion(4821, false));

    mockMvc
        .perform(post("/api/contacto").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.numero").value(4821));
  }

  @Test
  void recibir_sinCorreo_responde400ConCodigoDelCatalogo() throws Exception {
    String sinCorreo =
        """
        {"tema": "OTRO", "texto": "Hola", "claveEnvio": "6f1c2b9e-3d4a-4b5c-8d6e-7f8091a2b3c4"}
        """;

    mockMvc
        .perform(post("/api/contacto").contentType(MediaType.APPLICATION_JSON).content(sinCorreo))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"));
    verify(service, never()).recibir(any(), any());
  }

  @Test
  void recibir_conTextoDeMasDe2000Caracteres_responde400() throws Exception {
    String largo =
        "{\"tema\": \"OTRO\", \"texto\": \""
            + "a".repeat(2001)
            + "\", \"correo\": \"ana@ejemplo.org\","
            + " \"claveEnvio\": \"6f1c2b9e-3d4a-4b5c-8d6e-7f8091a2b3c4\"}";

    mockMvc
        .perform(post("/api/contacto").contentType(MediaType.APPLICATION_JSON).content(largo))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"));
  }

  @Test
  void recibir_conCorreoInvalidoOTemaDesconocido_responde400() throws Exception {
    mockMvc
        .perform(
            post("/api/contacto")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CUERPO.replace("ana@ejemplo.org", "no-es-un-correo")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"));
    mockMvc
        .perform(
            post("/api/contacto")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CUERPO.replace("ENTREGA", "QUEJA")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"));
  }

  @Test
  void recibir_sobreElLimite_responde429ConCodigoDelCatalogo() throws Exception {
    when(service.recibir(any(), any()))
        .thenThrow(new BusinessStateException(ErrorCatalog.CONTACTO_LIMITE_EXCEDIDO));

    mockMvc
        .perform(post("/api/contacto").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.code").value("ERR-EST-903"));
  }

  @Test
  void listar_devuelveLaPaginaConElTotalEnElHeader() throws Exception {
    when(service.listar(EstadoMensajeContacto.RECIBIDO, 1))
        .thenReturn(new ContactoService.Pagina(List.of(mensaje(5, "RECIBIDO")), 21));

    mockMvc
        .perform(get("/api/contacto").param("estado", "RECIBIDO").param("pagina", "1"))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Total-Count", "21"))
        .andExpect(jsonPath("$[0].numero").value(5))
        .andExpect(jsonPath("$[0].estado").value("RECIBIDO"));
  }

  @Test
  void listar_conEstadoDesconocido_responde400() throws Exception {
    mockMvc
        .perform(get("/api/contacto").param("estado", "LEIDO"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void obtener_responde200O404() throws Exception {
    when(service.obtener(5)).thenReturn(Optional.of(mensaje(5, "RECIBIDO")));
    when(service.obtener(6)).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/api/contacto/5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.correo").value("ana@ejemplo.org"));
    mockMvc.perform(get("/api/contacto/6")).andExpect(status().isNotFound());
  }

  @Test
  void responder_devuelveElMensajeConSuEstadoO404() throws Exception {
    when(service.responder(5, "Listo")).thenReturn(Optional.of(mensaje(5, "RESPONDIDO")));
    when(service.responder(6, "Listo")).thenReturn(Optional.empty());

    mockMvc
        .perform(
            post("/api/contacto/5/respuesta")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"texto\": \"Listo\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("RESPONDIDO"));
    mockMvc
        .perform(
            post("/api/contacto/6/respuesta")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"texto\": \"Listo\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void responder_conTextoVacio_responde400() throws Exception {
    mockMvc
        .perform(
            post("/api/contacto/5/respuesta")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"texto\": \" \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"));
  }
}
