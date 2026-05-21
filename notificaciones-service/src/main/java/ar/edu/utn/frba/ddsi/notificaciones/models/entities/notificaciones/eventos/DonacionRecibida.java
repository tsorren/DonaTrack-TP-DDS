package ar.edu.utn.frba.ddsi.notificaciones.models.entities.notificaciones.eventos;

public class DonacionRecibida extends EventoDeDonacion {
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
