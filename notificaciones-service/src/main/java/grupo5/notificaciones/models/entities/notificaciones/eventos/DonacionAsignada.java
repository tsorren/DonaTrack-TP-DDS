package grupo5.notificaciones.models.entities.notificaciones.eventos;

public class DonacionAsignada extends EventoDeDonacion {
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
