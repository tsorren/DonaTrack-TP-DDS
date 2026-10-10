package grupo5.notificaciones.infrastructure;

import grupo5.notificaciones.models.entities.personas.Correo;
import grupo5.notificaciones.models.entities.personas.Telefono;
import grupo5.notificaciones.models.entities.personas.TipoTelefono;
import grupo5.notificaciones.models.ports.NotificacionSender;
import org.springframework.stereotype.Component;

@Component
public class NotificacionRouter implements NotificacionSender {

  private final CorreoAdapter correoApi;
  private final TelefonoAdapter telefonoApi;
  private final WhatsAppAdapter whatsappApi;

  public NotificacionRouter(
      CorreoAdapter correoApi, TelefonoAdapter telefonoApi, WhatsAppAdapter whatsappApi) {
    this.correoApi = correoApi;
    this.telefonoApi = telefonoApi;
    this.whatsappApi = whatsappApi;
  }

  @Override
  public boolean enviarA(Correo correo, String mensaje) {
    return this.correoApi.enviarMail(correo.getDireccionCorreo(), mensaje);
  }

  @Override
  public boolean enviarA(Telefono telefono, String mensaje) {
    if (telefono.getTipo() == TipoTelefono.WHATSAPP) {
      return this.whatsappApi.enviarWhatsApp(telefono.obtenerNumeroCompleto(), mensaje);
    } else {
      return this.telefonoApi.enviarSms(telefono.obtenerNumeroCompleto(), mensaje);
    }
  }
}
