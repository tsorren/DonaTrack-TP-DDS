package grupo5.notificaciones.infrastructure.amqp;

import static org.assertj.core.api.Assertions.assertThat;

import grupo5.notificaciones.config.RabbitMQConfig;
import grupo5.notificaciones.dto.input.EventoCuentaEnlaceSolicitadoV1;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;

/**
 * El contrato con el SPI de correo de Keycloak (SPEC-06 E2-06). {@code JSON_DEL_SPI} es, letra por
 * letra, lo que produce {@code EventoEnlaceDeCuenta.aJson()} en {@code auth-service/spi} para estos
 * valores: si cambia un lado y no el otro, este test lo avisa.
 */
class EventoCuentaEnlaceSolicitadoContratoTest {

  private static final String JSON_DEL_SPI =
      "{\"tipo\":\"RECUPERAR_CLAVE\",\"cuentaId\":\"8b754922-eff7-4b15-9d38-d91515e5363b\","
          + "\"destinatario\":\"ana@example.com\",\"enlace\":\"http://localhost:8180/realms/"
          + "donatrack/login-actions/action-token?key=abc&client_id=cliente-liviano\","
          + "\"venceEnMinutos\":30,\"idioma\":\"es\",\"fecha\":\"2026-11-12T02:07:56Z\"}";

  private final RabbitMQConfig config = new RabbitMQConfig();
  private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

  private Object leer(String json) {
    JacksonJsonMessageConverter convertidor = config.messageConverter(config.classMapper());
    MessageProperties propiedades = new MessageProperties();
    propiedades.setContentType("application/json");
    propiedades.setHeader("__TypeId__", "cuenta.enlace-solicitado.v1");
    return convertidor.fromMessage(new Message(json.getBytes(StandardCharsets.UTF_8), propiedades));
  }

  @Test
  void elConvertidorDeNotificacionesLeeLoQuePublicaElSpi() {
    Object leido = leer(JSON_DEL_SPI);

    assertThat(leido).isInstanceOf(EventoCuentaEnlaceSolicitadoV1.class);
    EventoCuentaEnlaceSolicitadoV1 evento = (EventoCuentaEnlaceSolicitadoV1) leido;
    assertThat(evento.tipo()).isEqualTo("RECUPERAR_CLAVE");
    assertThat(evento.cuentaId())
        .isEqualTo(UUID.fromString("8b754922-eff7-4b15-9d38-d91515e5363b"));
    assertThat(evento.destinatario()).isEqualTo("ana@example.com");
    assertThat(evento.enlace()).endsWith("action-token?key=abc&client_id=cliente-liviano");
    assertThat(evento.venceEnMinutos()).isEqualTo(30);
    assertThat(evento.idioma()).isEqualTo("es");
    assertThat(evento.fecha()).isEqualTo(Instant.parse("2026-11-12T02:07:56Z"));
  }

  @Test
  void loQuePublicaElSpiPasaLaValidacionDelEvento() {
    assertThat(validador.validate((EventoCuentaEnlaceSolicitadoV1) leer(JSON_DEL_SPI))).isEmpty();
  }

  @Test
  void unEnlaceConEsquemaPeligrosoNoPasaLaValidacion() {
    String peligroso =
        JSON_DEL_SPI.replaceFirst("http://localhost:8180[^\"]*", "javascript:alert(1)");

    assertThat(validador.validate((EventoCuentaEnlaceSolicitadoV1) leer(peligroso))).isNotEmpty();
  }
}
