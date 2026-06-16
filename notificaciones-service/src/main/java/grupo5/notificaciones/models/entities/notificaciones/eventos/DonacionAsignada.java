package grupo5.notificaciones.models.entities.notificaciones.eventos;

import grupo5.notificaciones.models.entities.personas.Persona;
import java.time.LocalDateTime;

public class DonacionAsignada extends EventoDeDonacion {
  public DonacionAsignada(
      Persona persona, Persona entidadBeneficiaria, String detalleDonacion, LocalDateTime fecha) {
    super();
  }

  public DonacionAsignada() {}

  @Override
  protected String armarMensajeDonante() {
    return "Tu donación de "
        + getDetalleDonacion()
        + " fue asignada a "
        + getEntidadBeneficiaria().getDenominacion();
  }

  @Override
  protected String armarMensajeBeneficiario() {
    return "Se te ha asignado la donación del donante "
        + getPersona().getDenominacion()
        + ". Contiene: "
        + getDetalleDonacion();
  }
}
