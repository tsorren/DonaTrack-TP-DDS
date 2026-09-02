package grupo5.notificaciones.models.entities.notificaciones.eventos;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.personas.Persona;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;

@Getter
public class DonacionVencida extends EventoNotificable {
  private final Persona administracion;
  private final String detalleDonacion;
  private final String motivo;

  public DonacionVencida(
      Persona donante,
      Persona administracion,
      String detalleDonacion,
      String motivo,
      LocalDateTime fecha) {
    super(donante, fecha);
    if (administracion == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    if (detalleDonacion == null || detalleDonacion.isBlank()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    if (motivo == null || motivo.isBlank()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    this.administracion = administracion;
    this.detalleDonacion = detalleDonacion;
    this.motivo = motivo;
  }

  @Override
  public List<Notificacion> generarNotificaciones() {
    String mensaje =
        "Atención administrador: La donación de "
            + detalleDonacion
            + " del donante "
            + getPersona().getDenominacion()
            + " ha vencido. Motivo: "
            + motivo
            + ".";
    Notificacion aAdmin = new Notificacion(this.administracion.getId(), mensaje);
    return List.of(aAdmin);
  }
}
