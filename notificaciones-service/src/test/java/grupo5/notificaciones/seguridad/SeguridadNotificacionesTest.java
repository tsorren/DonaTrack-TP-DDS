package grupo5.notificaciones.seguridad;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import grupo5.common.CommonLibAutoConfiguration;
import grupo5.common.logging.LoggingAutoConfiguration;
import grupo5.common.seguridad.SeguridadAutoConfiguration;
import grupo5.notificaciones.controllers.ContactoController;
import grupo5.notificaciones.controllers.NotificacionController;
import grupo5.notificaciones.controllers.PersonasController;
import grupo5.notificaciones.dto.NotificacionDTO;
import grupo5.notificaciones.dto.PersonaReplicaDTO;
import grupo5.notificaciones.dto.input.EventoDonanteRegistradoDTO;
import grupo5.notificaciones.dto.input.EventoNotificableDTO;
import grupo5.notificaciones.models.entities.personas.TipoPersona;
import grupo5.notificaciones.services.IPersonasService;
import grupo5.notificaciones.services.impl.ContactoService;
import grupo5.notificaciones.services.impl.NotificacionService;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Autorización de Notificaciones con la seguridad encendida (matriz de permisos, §8): sin token es
 * 401; con token, el rol, el permiso y el dueño. El servicio va simulado, así que un 200, un 202 o
 * un 204 quiere decir "pasó la autorización", y un 403, que no. Un cuerpo inválido daría 400 antes
 * de evaluar el permiso, por eso los pedidos con cuerpo llevan uno válido.
 */
@WebMvcTest({NotificacionController.class, PersonasController.class, ContactoController.class})
@Import({
  CommonLibAutoConfiguration.class,
  LoggingAutoConfiguration.class,
  SeguridadAutoConfiguration.class,
  DuenioDe.class,
  ReglasDeAccesoNotificaciones.class
})
@ImportAutoConfiguration(AopAutoConfiguration.class)
@TestPropertySource(properties = "donatrack.seguridad.habilitada=true")
class SeguridadNotificacionesTest {

  private static final UUID DONANTE = UUID.randomUUID();
  private static final UUID ENTIDAD = UUID.randomUUID();
  private static final UUID AJENO = UUID.randomUUID();
  private static final UUID NOTIFICACION_PROPIA = UUID.randomUUID();
  private static final UUID NOTIFICACION_AJENA = UUID.randomUUID();
  private static final UUID NOTIFICACION_INEXISTENTE = UUID.randomUUID();
  private static final LocalDateTime FECHA = LocalDateTime.of(2026, 6, 18, 12, 0, 0);
  private static final String NOTIFICACIONES = "/api/notificaciones";
  private static final String PERSONAS = "/api/notificaciones/personas";
  private static final String CONTACTO = "/api/contacto";

  private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

  @Autowired private MockMvc mockMvc;

  @MockitoBean private NotificacionService notificacionService;
  @MockitoBean private IPersonasService personasService;
  @MockitoBean private ContactoService contactoService;

  @TestConfiguration
  @EnableWebSecurity
  static class Tokens {

    @Bean
    JwtDecoder decodificador() {
      return valor ->
          switch (valor) {
            case "donante" -> token("DONANTE", DONANTE, "VER_NOTIFICACIONES");
            case "donante-sin-permisos" -> token("DONANTE", DONANTE);
            case "entidad" -> token("ENTIDAD", ENTIDAD, "VER_NOTIFICACIONES");
            case "admin" -> token("ADMIN", AJENO);
            case "servicio" -> token("SERVICIO", AJENO);
            default -> throw new BadJwtException("token desconocido");
          };
    }

    private static Jwt token(String rol, UUID personaId, String... permisos) {
      return Jwt.withTokenValue("t")
          .header("alg", "none")
          .claim("roles", List.of(rol))
          .claim("personaId", personaId.toString())
          .claim("permisos", List.of(permisos))
          .build();
    }
  }

  @BeforeEach
  void duenos() {
    NotificacionDTO dto = new NotificacionDTO(NOTIFICACION_PROPIA, "mensaje", "PENDIENTE", FECHA);
    when(notificacionService.personaDe(NOTIFICACION_PROPIA)).thenReturn(Optional.of(DONANTE));
    when(notificacionService.personaDe(NOTIFICACION_AJENA)).thenReturn(Optional.of(AJENO));
    when(notificacionService.marcarComoLeida(NOTIFICACION_PROPIA)).thenReturn(Optional.of(dto));
    when(notificacionService.marcarComoLeida(NOTIFICACION_AJENA)).thenReturn(Optional.of(dto));
    when(contactoService.recibir(any(), isNull()))
        .thenReturn(new ContactoService.Recepcion(1, true));
    when(contactoService.listar(isNull(), anyInt()))
        .thenReturn(new ContactoService.Pagina(List.of(), 0));
    when(contactoService.obtener(anyLong())).thenReturn(Optional.empty());
    when(contactoService.responder(anyLong(), anyString())).thenReturn(Optional.empty());
  }

  private ResultActions pedir(MockHttpServletRequestBuilder pedido, String token) throws Exception {
    return mockMvc.perform(
        token == null ? pedido : pedido.header("Authorization", "Bearer " + token));
  }

  private static String historialDe(UUID persona) {
    return NOTIFICACIONES + "/persona/" + persona;
  }

  private String evento() throws Exception {
    EventoNotificableDTO dto = new EventoDonanteRegistradoDTO(AJENO, DONANTE, FECHA, "user123");
    return json.writerFor(EventoNotificableDTO.class).writeValueAsString(dto);
  }

  private String persona() throws Exception {
    return json.writeValueAsString(
        new PersonaReplicaDTO(
            UUID.randomUUID(), "Persona de prueba", TipoPersona.HUMANA, Collections.emptyList()));
  }

  @Test
  void sinTokenResponde401ConElErrorComun() throws Exception {
    pedir(get(historialDe(DONANTE)), null)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("ERR-CSR-401"));
  }

  @Test
  void tokenInvalidoResponde401() throws Exception {
    pedir(get(historialDe(DONANTE)), "trucho").andExpect(status().isUnauthorized());
  }

  @Test
  void elDonanteVeYMarcaSusNotificaciones() throws Exception {
    pedir(get(historialDe(DONANTE)), "donante").andExpect(status().isOk());
    pedir(get(historialDe(DONANTE) + "/no-leidas/conteo"), "donante").andExpect(status().isOk());
    pedir(patch(historialDe(DONANTE) + "/marcar-todas-leidas"), "donante")
        .andExpect(status().isNoContent());
  }

  @Test
  void laEntidadVeSusNotificaciones() throws Exception {
    pedir(get(historialDe(ENTIDAD)), "entidad").andExpect(status().isOk());
  }

  @Test
  void nadieVeLasNotificacionesDeOtraPersona() throws Exception {
    pedir(get(historialDe(AJENO)), "donante").andExpect(status().isForbidden());
    pedir(get(historialDe(AJENO) + "/no-leidas/conteo"), "donante")
        .andExpect(status().isForbidden());
    pedir(patch(historialDe(AJENO) + "/marcar-todas-leidas"), "donante")
        .andExpect(status().isForbidden());
    pedir(get(historialDe(DONANTE)), "entidad").andExpect(status().isForbidden());
  }

  @Test
  void sinElPermisoResponde403AunqueSeaSuPersona() throws Exception {
    pedir(get(historialDe(DONANTE)), "donante-sin-permisos").andExpect(status().isForbidden());
  }

  @Test
  void laCuentaDeServicioNoLeeNotificacionesDeUnaPersona() throws Exception {
    pedir(get(historialDe(DONANTE)), "servicio").andExpect(status().isForbidden());
  }

  @Test
  void elAdminVeLasNotificacionesDeCualquiera() throws Exception {
    pedir(get(historialDe(DONANTE)), "admin").andExpect(status().isOk());
    pedir(get(historialDe(DONANTE) + "/no-leidas/conteo"), "admin").andExpect(status().isOk());
  }

  @Test
  void elDonanteMarcaComoLeidaSoloUnaNotificacionSuya() throws Exception {
    String propia = NOTIFICACIONES + "/" + NOTIFICACION_PROPIA + "/leida";
    String ajena = NOTIFICACIONES + "/" + NOTIFICACION_AJENA + "/leida";
    pedir(patch(propia), "donante").andExpect(status().isOk());
    pedir(patch(ajena), "donante").andExpect(status().isForbidden());
  }

  @Test
  void marcarLeidaPideElPermisoAunqueLaNotificacionSeaSuya() throws Exception {
    String propia = NOTIFICACIONES + "/" + NOTIFICACION_PROPIA + "/leida";
    pedir(patch(propia), "donante-sin-permisos").andExpect(status().isForbidden());
  }

  @Test
  void unaNotificacionInexistenteNoDelataSuExistenciaAQuienNoEsAdmin() throws Exception {
    String inexistente = NOTIFICACIONES + "/" + NOTIFICACION_INEXISTENTE + "/leida";
    pedir(patch(inexistente), "donante").andExpect(status().isForbidden());
    pedir(patch(inexistente), "admin").andExpect(status().isNotFound());
  }

  @Test
  void elAdminMarcaComoLeidaLaNotificacionDeCualquiera() throws Exception {
    String ajena = NOTIFICACIONES + "/" + NOTIFICACION_AJENA + "/leida";
    pedir(patch(ajena), "admin").andExpect(status().isOk());
  }

  @Test
  void elListadoGeneralYLaConsultaPorIdSonDelAdmin() throws Exception {
    pedir(get(NOTIFICACIONES), "donante").andExpect(status().isForbidden());
    pedir(get(NOTIFICACIONES), "servicio").andExpect(status().isForbidden());
    pedir(get(NOTIFICACIONES), "admin").andExpect(status().isOk());
    pedir(get(NOTIFICACIONES + "/" + NOTIFICACION_PROPIA), "donante")
        .andExpect(status().isForbidden());
    pedir(get(NOTIFICACIONES + "/" + NOTIFICACION_PROPIA), "admin")
        .andExpect(status().isNotFound());
  }

  @Test
  void elAliasLegadoDelHistorialEsSoloDelAdmin() throws Exception {
    String alias = "/notificaciones/persona/" + DONANTE;
    pedir(get(alias), "donante").andExpect(status().isForbidden());
    pedir(get(alias), "servicio").andExpect(status().isForbidden());
    pedir(get(alias), "admin").andExpect(status().isOk());
  }

  @Test
  void soloElServicioYElAdminPublicanEventos() throws Exception {
    for (String ruta : List.of(NOTIFICACIONES + "/eventos", "/notificaciones")) {
      pedir(post(ruta).contentType(MediaType.APPLICATION_JSON).content(evento()), "donante")
          .andExpect(status().isForbidden());
      pedir(post(ruta).contentType(MediaType.APPLICATION_JSON).content(evento()), "entidad")
          .andExpect(status().isForbidden());
      pedir(post(ruta).contentType(MediaType.APPLICATION_JSON).content(evento()), "servicio")
          .andExpect(status().isAccepted());
      pedir(post(ruta).contentType(MediaType.APPLICATION_JSON).content(evento()), "admin")
          .andExpect(status().isAccepted());
    }
  }

  @Test
  void laReplicaDePersonasEsDelServicioYDelAdmin() throws Exception {
    String propia = PERSONAS + "/" + DONANTE;
    pedir(get(propia), "donante").andExpect(status().isForbidden());
    pedir(get(propia), "servicio").andExpect(status().isOk());
    pedir(get(propia), "admin").andExpect(status().isOk());
    pedir(delete(propia), "donante").andExpect(status().isForbidden());
    pedir(delete(propia), "servicio").andExpect(status().isNoContent());
    pedir(put(PERSONAS).contentType(MediaType.APPLICATION_JSON).content(persona()), "donante")
        .andExpect(status().isForbidden());
    pedir(put(PERSONAS).contentType(MediaType.APPLICATION_JSON).content(persona()), "servicio")
        .andExpect(status().isOk());
  }

  @Test
  void elFormularioDeContactoNoPideToken() throws Exception {
    pedir(post(CONTACTO).contentType(MediaType.APPLICATION_JSON).content(mensajeContacto()), null)
        .andExpect(status().isCreated());
  }

  @Test
  void losMensajesDeContactoSonDelAdmin() throws Exception {
    String respuesta = "{\"texto\":\"Gracias por escribir\"}";
    pedir(get(CONTACTO), null).andExpect(status().isForbidden());
    pedir(get(CONTACTO + "/1"), null).andExpect(status().isUnauthorized());
    pedir(get(CONTACTO), "donante").andExpect(status().isForbidden());
    pedir(get(CONTACTO + "/1"), "entidad").andExpect(status().isForbidden());
    pedir(
            post(CONTACTO + "/1/respuesta")
                .contentType(MediaType.APPLICATION_JSON)
                .content(respuesta),
            "donante")
        .andExpect(status().isForbidden());
    pedir(get(CONTACTO), "admin").andExpect(status().isOk());
    pedir(get(CONTACTO + "/1"), "admin").andExpect(status().isNotFound());
    pedir(
            post(CONTACTO + "/1/respuesta")
                .contentType(MediaType.APPLICATION_JSON)
                .content(respuesta),
            "admin")
        .andExpect(status().isNotFound());
  }

  private static String mensajeContacto() {
    return "{\"tema\":\"ENTREGA\",\"texto\":\"Consulta de prueba\","
        + "\"correo\":\"persona@example.com\",\"claveEnvio\":\""
        + UUID.randomUUID()
        + "\"}";
  }
}
