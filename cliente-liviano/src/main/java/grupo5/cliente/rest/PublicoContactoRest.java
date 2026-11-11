package grupo5.cliente.rest;

import grupo5.cliente.puertos.ContactoRechazado;
import grupo5.cliente.puertos.PublicoContactoPuerto;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Contacto con Notificaciones ({@code POST /api/contacto}, HB-21, contrato de SPEC-08). El 400 y el
 * 429 son respuestas que la pantalla explica; el resto de las fallas va a {@code ErroresHttp}.
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.contacto", havingValue = "rest")
class PublicoContactoRest implements PublicoContactoPuerto {

  private final NotificacionesApi notificaciones;

  PublicoContactoRest(NotificacionesApi notificaciones) {
    this.notificaciones = notificaciones;
  }

  @Override
  public UUID nuevaClave() {
    return UUID.randomUUID();
  }

  @Override
  public Long numeroDeEjemplo() {
    return null;
  }

  @Override
  public long enviar(Mensaje mensaje, String ipOrigen) {
    try {
      return notificaciones
          .contacto(
              ipOrigen,
              new NotificacionesApi.MensajeDeContacto(
                  mensaje.tema(),
                  mensaje.texto(),
                  mensaje.codigoError(),
                  mensaje.correo(),
                  mensaje.claveEnvio()))
          .numero();
    } catch (HttpClientErrorException.BadRequest invalido) {
      throw new ContactoRechazado(Motivo.INVALIDO, invalido);
    } catch (HttpClientErrorException.TooManyRequests limite) {
      throw new ContactoRechazado(Motivo.LIMITE, limite);
    }
  }
}
