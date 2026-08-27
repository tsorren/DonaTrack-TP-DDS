package grupo5.notificaciones.models.entities.notificaciones;

import grupo5.common.repositories.AggregateRoot;
import grupo5.notificaciones.models.entities.notificaciones.eventos.CambioEstadoNotificacion;
import grupo5.notificaciones.models.entities.notificaciones.events.NotificacionCreada;
import grupo5.notificaciones.models.entities.notificaciones.events.NotificacionDomainEvent;
import grupo5.notificaciones.models.entities.notificaciones.events.NotificacionEnviada;
import grupo5.notificaciones.models.entities.notificaciones.events.NotificacionFallida;
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
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class Notificacion implements Anonimizable, AggregateRoot {
  private UUID id;
  private UUID personaId;
  private String mensaje;
  private LocalDateTime fechaCreacion;
  private EstadoNotificacion estadoNotificacion;

  @Getter(AccessLevel.NONE)
  private List<CambioEstadoNotificacion> historialEstado;

  @Getter(AccessLevel.NONE)
  private final transient List<NotificacionDomainEvent> domainEvents = new ArrayList<>();

  public Notificacion(UUID personaId, String mensaje) {
    this.id = UUID.randomUUID();
    this.personaId = personaId;
    this.mensaje = mensaje;
    this.fechaCreacion = LocalDateTime.now(ZoneId.systemDefault());
    this.historialEstado = new ArrayList<>();
    this.actualizarEstado(
        EstadoNotificacion.PENDIENTE); // usa el método para que quede en el historial
  }

  public List<CambioEstadoNotificacion> getHistorialEstado() {
    return List.copyOf(this.historialEstado);
  }

  public List<NotificacionDomainEvent> getDomainEvents() {
    return List.copyOf(this.domainEvents);
  }

  public void clearDomainEvents() {
    this.domainEvents.clear();
  }

  public void actualizarEstado(EstadoNotificacion nuevoEstado) {
    EstadoNotificacion anterior = this.estadoNotificacion;
    this.estadoNotificacion = nuevoEstado;
    LocalDateTime timestamp = LocalDateTime.now(ZoneId.systemDefault());
    this.historialEstado.add(new CambioEstadoNotificacion(anterior, nuevoEstado, timestamp));
    this.registrarDomainEvent(nuevoEstado, timestamp);
  }

  private void registrarDomainEvent(EstadoNotificacion nuevoEstado, LocalDateTime timestamp) {
    switch (nuevoEstado) {
      case PENDIENTE -> this.domainEvents.add(
          new NotificacionCreada(this.id, this.personaId, timestamp));
      case ENVIADA -> this.domainEvents.add(new NotificacionEnviada(this.id, timestamp));
      case FALLIDA -> this.domainEvents.add(new NotificacionFallida(this.id, timestamp));
    }
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
