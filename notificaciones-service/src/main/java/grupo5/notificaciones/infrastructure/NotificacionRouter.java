package grupo5.notificaciones.infrastructure;

import grupo5.notificaciones.models.entities.medioDeContacto.Correo;
import grupo5.notificaciones.models.entities.medioDeContacto.NotificacionSender;
import grupo5.notificaciones.models.entities.medioDeContacto.Telefono;
import grupo5.notificaciones.models.entities.medioDeContacto.WhatsApp;

public class NotificacionRouter implements NotificacionSender {

  // Tiene como dependencias a las clases separadas
  private CorreoAdapter correoApi;
  private TelefonoAdapter telefonoApi;
  private WhatsAppAdapter whatsappApi;

  // Se inyectan por constructor
  public NotificacionRouter(
      CorreoAdapter correoApi, TelefonoAdapter telefonoApi, WhatsAppAdapter whatsappApi) {
    this.correoApi = correoApi;
    this.telefonoApi = telefonoApi;
    this.whatsappApi = whatsappApi;
  }

  // Métodos del Double Dispatch: actúan como un simple "pasamanos"
  @Override
  public boolean enviarA(Correo correo, String mensaje) {
    return this.correoApi.enviarMail(correo.getDireccionCorreo(), mensaje);
  }

  @Override
  public boolean enviarA(Telefono telefono, String mensaje) {
    return this.telefonoApi.enviarSms(telefono.obtenerNumeroCompleto(), mensaje);
  }

  @Override
  public boolean enviarA(WhatsApp whatsapp, String mensaje) {
    return this.whatsappApi.enviarWhatsApp(whatsapp.obtenerNumeroCompleto(), mensaje);
  }
}
