package grupo5.notificaciones.models.entities.notificaciones.eventos;

import grupo5.notificaciones.models.entities.personas.Persona;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.Getter;

@Getter
public class DonacionRecibida extends EventoDeDonacion {

  private static final DateTimeFormatter FORMATO_COMPROBANTE =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  private final String patenteCamion;

  public DonacionRecibida(
      Persona persona,
      Persona entidadBeneficiaria,
      String detalleDonacion,
      String patenteCamion,
      LocalDateTime fecha) {
    super(persona, entidadBeneficiaria, detalleDonacion, fecha);
    this.patenteCamion = patenteCamion;
  }

  @Override
  protected String armarMensajeDonante() {
    return "¡Gracias! "
        + getEntidadBeneficiaria().getDenominacion()
        + " recibió tu donación de "
        + getDetalleDonacion()
        + ". "
        + armarComprobante();
  }

  @Override
  protected String armarMensajeBeneficiario() {
    return "Ya llegó la donación " + getDetalleDonacion() + " a tu sede. " + armarComprobante();
  }

  private String armarComprobante() {
    return "Comprobante de entrega: "
        + getFecha().format(FORMATO_COMPROBANTE)
        + " - camión "
        + patenteCamion;
  }
}
