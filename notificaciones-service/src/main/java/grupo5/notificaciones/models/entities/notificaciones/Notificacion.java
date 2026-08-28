package grupo5.notificaciones.models.entities.notificaciones;

import grupo5.common.events.AgregadoConEventos;
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

// Oleada 11: extiende AgregadoConEventos<NotificacionDomainEvent> (common-lib) en vez de
// mantener su propia lista de domainEvents — registrarEvento()/getDomainEvents()/
// clearDomainEvents() vienen heredados; AggregateRoot también lo aporta la clase base.
@Getter
public class Notificacion extends AgregadoConEventos<NotificacionDomainEvent>
    implements Anonimizable {
  private UUID id;
  private UUID personaId;
  private String mensaje;
  private LocalDateTime fechaCreacion;
  private EstadoNotificacion estadoNotificacion;

  @Getter(AccessLevel.NONE)
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

  public List<CambioEstadoNotificacion> getHistorialEstado() {
    return List.copyOf(this.historialEstado);
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
      case PENDIENTE -> this.registrarEvento(
          new NotificacionCreada(this.id, this.personaId, timestamp));
      case ENVIADA -> this.registrarEvento(new NotificacionEnviada(this.id, timestamp));
      case FALLIDA -> this.registrarEvento(new NotificacionFallida(this.id, timestamp));
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
    // Guard de esPredeterminado == null (RF-07, Oleada 9.5): Boolean.TRUE.equals(...) nunca lanza
    // NullPointerException, a diferencia de
    // Comparator.comparing(MedioDeContacto::getEsPredeterminado)
    // sobre un Boolean nulo. Un null se trata como "no predeterminado" — mismo criterio que ya usa
    // MedioDeContactoMapper.toEntity() para el mapeo inverso.
    // Desempate explícito: Stream.sorted() está garantizado estable, así que dos medios empatados
    // en
    // esPredeterminado conservan el orden de alta (posición en Persona.mediosDeContacto) — se deja
    // como criterio deliberado y documentado, no como un accidente de la implementación del sort.
    return persona.getMediosDeContacto().stream()
        .sorted(
            Comparator.comparing(
                    (MedioDeContacto m) -> Boolean.TRUE.equals(m.getEsPredeterminado()))
                .reversed())
        .toList();
  }

  @Override
  public void anonimizar() {
    this.mensaje = Anonimizable.VALOR_STRING;
  }
}
