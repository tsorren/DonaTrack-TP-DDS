package grupo5.notificaciones.models.entities.notificaciones.eventos;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.personas.Persona;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;

@Getter
public abstract class EventoDeDonacion extends EventoNotificable {
  private final Persona entidadBeneficiaria;
  private final String detalleDonacion;

  protected EventoDeDonacion(
      Persona persona, Persona entidadBeneficiaria, String detalleDonacion, LocalDateTime fecha) {
    super(persona, fecha);
    if (entidadBeneficiaria == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    if (detalleDonacion == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    this.entidadBeneficiaria = entidadBeneficiaria;
    this.detalleDonacion = detalleDonacion;
  }

  @Override
  public List<Notificacion> generarNotificaciones() {
    Notificacion notificacionDonante =
        new Notificacion(this.getPersona().getId(), this.armarMensajeDonante());

    Notificacion notificacionBeneficiario =
        new Notificacion(this.entidadBeneficiaria.getId(), this.armarMensajeBeneficiario());

    return List.of(notificacionDonante, notificacionBeneficiario);
  }

  protected abstract String armarMensajeDonante();

  protected abstract String armarMensajeBeneficiario();
}
