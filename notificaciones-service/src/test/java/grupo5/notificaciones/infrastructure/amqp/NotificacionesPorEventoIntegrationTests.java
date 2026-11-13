package grupo5.notificaciones.infrastructure.amqp;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.notificaciones.config.RabbitMQConfig;
import grupo5.notificaciones.dto.input.EventoDonacionAsignadaV1;
import grupo5.notificaciones.dto.input.EventoDonacionRecibidaV1;
import grupo5.notificaciones.dto.input.EventoIncentivoMisionCumplidaV1;
import grupo5.notificaciones.dto.input.EventoIncentivoSubioCategoriaV1;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.entities.personas.TipoPersona;
import grupo5.notificaciones.models.repositories.IPersonaRepository;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * HB-09 (RF-08, RF-13): cada evento que el donante y la entidad deben ver termina en una
 * notificación visible en GET /api/notificaciones/persona/{personaId}. Recorre el contexto real:
 * JSON del contrato AMQP → convertidor con el mapeo de __TypeId__ → listener → servicio →
 * repositorio → API REST. Sin broker: el mensaje se convierte con el bean del convertidor y se
 * entrega al listener; la cola de RabbitMQ queda para los tests con Docker.
 */
@SpringBootTest
@AutoConfigureMockMvc
class NotificacionesPorEventoIntegrationTests {

  private static final String FECHA = "2026-10-16T10:00:00Z";
  private static final String PATENTE = "AF-123-ZZ";
  private static final String BANDEJA = "/api/notificaciones/persona/{personaId}";

  @Autowired private MockMvc mockMvc;
  @Autowired private JacksonJsonMessageConverter messageConverter;
  @Autowired private DonacionEventListener donacionListener;
  @Autowired private IncentivoEventListener incentivoListener;
  @Autowired private IPersonaRepository personaRepository;

  @Test
  void losEventosDelDonanteYDeLaEntidadQuedanVisiblesEnSuBandeja() throws Exception {
    UUID donante = guardarPersona("Carlos Donante", TipoPersona.HUMANA);
    UUID entidad = guardarPersona("Comedor Sol", TipoPersona.JURIDICA);

    donacionListener.onDonacionAsignada(
        convertir(
            RabbitMQConfig.ROUTING_KEY_DONACION_ASIGNADA,
            """
            {"donacionIndependienteId":"%s","donanteId":"%s","personaId":"%s","fecha":"%s",
             "personaBeneficiariaId":"%s","descripcion":"Ropa de abrigo",
             "destino":{"calle":"Calle Ficticia","altura":100,"codigoPostal":"C1043",
                        "localidad":"San Nicolás","provincia":"CABA","pais":"Argentina"},
             "pesoTotalKG":12.5,"volumenTotalM3":0.25}
            """
                .formatted(UUID.randomUUID(), donante, donante, FECHA, entidad),
            EventoDonacionAsignadaV1.class),
        UUID.randomUUID().toString());

    incentivoListener.onMisionCumplida(
        convertir(
            RabbitMQConfig.ROUTING_KEY_MISION_CUMPLIDA,
            """
            {"personaDonanteId":"%s","fecha":"%s","nombreMision":"Abrigo Solidario",
             "recompensa":"50 puntos"}
            """
                .formatted(donante, FECHA),
            EventoIncentivoMisionCumplidaV1.class),
        UUID.randomUUID().toString());

    incentivoListener.onSubioCategoria(
        convertir(
            RabbitMQConfig.ROUTING_KEY_SUBIO_CATEGORIA,
            """
            {"personaDonanteId":"%s","fecha":"%s","nombreNuevaCategoria":"Oro",
             "nombreViejaCategoria":"Plata"}
            """
                .formatted(donante, FECHA),
            EventoIncentivoSubioCategoriaV1.class),
        UUID.randomUUID().toString());

    donacionListener.onDonacionRecibida(
        convertir(
            RabbitMQConfig.ROUTING_KEY_DONACION_RECIBIDA,
            """
            {"donanteId":"%s","personaId":"%s","fecha":"%s","personaBeneficiariaId":"%s",
             "descripcion":"Ropa de abrigo","patenteCamion":"%s"}
            """
                .formatted(donante, donante, FECHA, entidad, PATENTE),
            EventoDonacionRecibidaV1.class),
        UUID.randomUUID().toString());

    // RF-08: asignación, misión cumplida, ascenso de categoría y recepción.
    mockMvc
        .perform(get(BANDEJA, donante))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(4)))
        .andExpect(algunMensajeContiene("fue asignada a Comedor Sol"))
        .andExpect(algunMensajeContiene("Completaste la misión Abrigo Solidario"))
        .andExpect(algunMensajeContiene("Has ascendido de la categoría Plata a Oro"))
        .andExpect(algunMensajeContiene("recibió tu donación", PATENTE));

    // RF-13: asignación y recepción.
    mockMvc
        .perform(get(BANDEJA, entidad))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(algunMensajeContiene("Se te ha asignado la donación del donante Carlos Donante"))
        .andExpect(algunMensajeContiene("Ya llegó la donación", PATENTE));
  }

  /** Alguna notificación de la bandeja tiene un mensaje que contiene todas las partes. */
  private static ResultMatcher algunMensajeContiene(String... partes) {
    return jsonPath(
        "$[*].mensaje",
        hasItem(
            allOf(
                Arrays.stream(partes)
                    .<Matcher<? super String>>map(Matchers::containsString)
                    .toList())));
  }

  private UUID guardarPersona(String denominacion, TipoPersona tipo) {
    Persona persona = new Persona(UUID.randomUUID(), new ArrayList<>(), denominacion, tipo);
    personaRepository.save(persona);
    return persona.getId();
  }

  /** Convierte el JSON con el mismo convertidor y __TypeId__ que usa el listener de RabbitMQ. */
  private <T> T convertir(String typeId, String json, Class<T> tipoEsperado) {
    MessageProperties props = new MessageProperties();
    props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
    props.setHeader("__TypeId__", typeId);
    Object convertido =
        messageConverter.fromMessage(new Message(json.getBytes(StandardCharsets.UTF_8), props));
    return tipoEsperado.cast(convertido);
  }
}
