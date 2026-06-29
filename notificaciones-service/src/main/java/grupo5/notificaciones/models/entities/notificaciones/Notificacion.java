package grupo5.notificaciones.models.entities.notificaciones;

import grupo5.common.repositories.AggregateRoot;
import grupo5.notificaciones.models.entities.personas.MedioDeContacto;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.ports.Anonimizable;
import grupo5.notificaciones.models.ports.NotificacionSender;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.Getter;

@Getter
public class Notificacion implements Anonimizable, AggregateRoot {
  private UUID id;
  private UUID personaId;
  private String mensaje;
  private LocalDateTime fechaCreacion;

  private EstadoNotificacion estadoNotificacion;

  public void actualizarEstado(EstadoNotificacion estadoNotificacion) {
    this.estadoNotificacion = estadoNotificacion;
  }

  public Notificacion(UUID personaId, String mensaje) {
    this.id = UUID.randomUUID();
    this.personaId = personaId;
    this.mensaje = mensaje;
    this.fechaCreacion = LocalDateTime.now(ZoneId.systemDefault());
    this.estadoNotificacion = EstadoNotificacion.PENDIENTE;
  }

  public void notificar(Persona persona, NotificacionSender sender) {
    if (persona == null) {
      this.estadoNotificacion = EstadoNotificacion.FALLIDA;
      return;
    }
    List<MedioDeContacto> medios = this.ordenarMedios(persona);

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

  private List<MedioDeContacto> ordenarMedios(Persona persona) {
    return persona.getMediosDeContacto().stream()
        .sorted(Comparator.comparing(MedioDeContacto::getEsPredeterminado).reversed())
        .toList();
  }

  @Override
  public void anonimizar() {
    this.mensaje = Anonimizable.VALOR_STRING;
  }
}
