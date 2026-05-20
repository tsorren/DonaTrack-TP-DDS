package ar.edu.utn.frba.ddsi.notificaciones.models.entities.notificaciones;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.medioDeContacto.*;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.persona.Persona;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Notificacion {
  private Persona persona;
  private String mensaje;
  private LocalDateTime fechaCreacion;
  private EstadoNotificacion estadoNotificacion;

  public Notificacion(Persona persona, String mensaje) {

    this.persona = persona;
    this.mensaje = mensaje;
    this.fechaCreacion = LocalDateTime.now();
    this.estadoNotificacion = EstadoNotificacion.PENDIENTE;
  }

  public void notificar() {
    List<MedioDeContacto> medios = this.ordenarMedios();

    for (MedioDeContacto medio : medios) {
      try {
        NotificacionSender sender = this.obtenerSender(medio);
        boolean enviado = medio.enviarMensaje(this.mensaje, sender);

        if (enviado) {
          this.estadoNotificacion = EstadoNotificacion.ENVIADA;
          return;
        }
      } catch (Exception e) {
        // sigue con el proximo medio
      }
    }
    this.estadoNotificacion = EstadoNotificacion.FALLIDA;
  }

  private NotificacionSender obtenerSender(MedioDeContacto medio) {

    if (medio instanceof Correo) {
      return new EmailSender();
    }

    if (medio instanceof WhatsApp) {
      return new WhatsAppSender();
    }

    if (medio instanceof Telefono) {
      return new SmsSender();
    }

    throw new RuntimeException("No existe sender");
  }

  private List<MedioDeContacto> ordenarMedios() {
    List<MedioDeContacto> medios = new ArrayList<>(persona.getContactos());

    medios.sort((m1, m2) -> Boolean.compare(m2.getEsPredeterminado(), m1.getEsPredeterminado()));
    return medios;
  }
}
