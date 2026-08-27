package grupo5.notificaciones.models.entities.notificaciones;

import grupo5.common.repositories.AggregateRoot;
import grupo5.notificaciones.models.entities.notificaciones.eventos.CambioEstadoNotificacion;
import grupo5.notificaciones.models.entities.personas.MedioDeContacto;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.ports.Anonimizable;
import grupo5.notificaciones.models.ports.NotificacionSender;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
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
  private List<CambioEstadoNotificacion> historialEstado;

  public Notificacion(UUID personaId, String mensaje) {
    this.id = UUID.randomUUID();
    this.personaId = personaId;
    this.mensaje = mensaje;
    this.fechaCreacion = LocalDateTime.now(ZoneId.systemDefault());
    this.historialEstado = new ArrayList<>();
    this.actualizarEstado(
        EstadoNotificacion.PENDIENTE); // usa el método para que quede en el historial
  }

  public void actualizarEstado(EstadoNotificacion nuevoEstado) {
    EstadoNotificacion anterior = this.estadoNotificacion;
    this.estadoNotificacion = nuevoEstado;
    this.historialEstado.add(
        new CambioEstadoNotificacion(
            anterior, nuevoEstado, LocalDateTime.now(ZoneId.systemDefault())));
  }

  public void notificar(Persona persona, NotificacionSender sender) {
    if (persona == null) {
      this.actualizarEstado(EstadoNotificacion.FALLIDA);
      return;
    }
    List<MedioDeContacto> medios = this.ordenarMedios(persona);

    for (MedioDeContacto medio : medios) {
      try {
        boolean enviado = medio.enviarMensaje(this.mensaje, sender);
        if (enviado) {
          this.actualizarEstado(EstadoNotificacion.ENVIADA);
          return;
        }
      } catch (Exception e) {
        // sigue con el próximo medio
      }
    }
    this.actualizarEstado(EstadoNotificacion.FALLIDA);
  }

  public List<MedioDeContacto> ordenarMedios(Persona persona) {
    return persona.getMediosDeContacto().stream()
        .sorted(Comparator.comparing(MedioDeContacto::getEsPredeterminado).reversed())
        .toList();
  }

  @Override
  public void anonimizar() {
    this.mensaje = Anonimizable.VALOR_STRING;
  }
}
