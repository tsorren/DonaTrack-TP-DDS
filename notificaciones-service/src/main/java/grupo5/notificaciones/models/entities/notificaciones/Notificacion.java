package grupo5.notificaciones.models.entities.notificaciones;

import grupo5.notificaciones.models.entities.personas.MedioDeContacto;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.ports.Anonimizable;
import grupo5.notificaciones.models.ports.NotificacionSender;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Notificacion implements Anonimizable {
  private Persona persona;
  private String mensaje;
  private LocalDateTime fechaCreacion;
  private EstadoNotificacion estadoNotificacion;

  public Notificacion(Persona persona, String mensaje) {

    this.persona = persona;
    this.mensaje = mensaje;
    this.fechaCreacion = LocalDateTime.now(ZoneId.systemDefault());
    this.estadoNotificacion = EstadoNotificacion.PENDIENTE;
  }

  public void notificar(NotificacionSender sender) {
    List<MedioDeContacto> medios = this.ordenarMedios();

    for (MedioDeContacto medio : medios) {
      try {
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

  private List<MedioDeContacto> ordenarMedios() {
    return this.persona.getMediosDeContacto().stream()
        .sorted(Comparator.comparing(MedioDeContacto::getEsPredeterminado).reversed())
        .toList();
  }

  @Override
  public void anonimizar() {
    this.mensaje = Anonimizable.VALOR_STRING;
    this.persona.anonimizar();
  }
}
