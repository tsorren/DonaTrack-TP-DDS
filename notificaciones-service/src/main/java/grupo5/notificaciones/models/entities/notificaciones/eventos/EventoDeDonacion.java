package grupo5.notificaciones.models.entities.notificaciones.eventos;

import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.persona.Persona;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class EventoDeDonacion extends EventoNotificable {
  private Persona entidadBeneficiaria;
  private String detalleDonacion;

  @Override
  public List<Notificacion> generarNotificaciones() {
    Notificacion notificacionDonante =
        new Notificacion(this.getPersona(), this.armarMensajeDonante());

    Notificacion notificacionBeneficiario =
        new Notificacion(this.entidadBeneficiaria, this.armarMensajeBeneficiario());

    return List.of(notificacionDonante, notificacionBeneficiario);
  }

  protected abstract String armarMensajeDonante();

  protected abstract String armarMensajeBeneficiario();
}
