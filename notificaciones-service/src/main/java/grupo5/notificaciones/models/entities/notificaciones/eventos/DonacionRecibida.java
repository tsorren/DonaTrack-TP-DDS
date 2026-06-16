package grupo5.notificaciones.models.entities.notificaciones.eventos;

import grupo5.notificaciones.models.entities.persona.Persona;
import java.time.LocalDateTime;

public class DonacionRecibida extends EventoDeDonacion {
  public DonacionRecibida(
      Persona persona, Persona entidadBeneficiaria, String detalleDonacion, LocalDateTime fecha) {
    super();
  }

  public DonacionRecibida() {}

  @Override
  protected String armarMensajeDonante() {
    return "¡Gracias! "
        + getEntidadBeneficiaria().getDenominacion()
        + " recibió tu donación de "
        + getDetalleDonacion();
  }

  @Override
  protected String armarMensajeBeneficiario() {
    return "Ya llegó la donación " + getDetalleDonacion() + " a tu sede";
  }
}
