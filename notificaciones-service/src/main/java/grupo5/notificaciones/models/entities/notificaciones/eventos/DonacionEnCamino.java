package grupo5.notificaciones.models.entities.notificaciones.eventos;

import grupo5.notificaciones.models.entities.personas.Persona;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DonacionEnCamino extends EventoDeDonacion {

  private String enlaceSeguimiento;

  public DonacionEnCamino(
      Persona persona,
      Persona entidadBeneficiaria,
      String detalleDonacion,
      String enlaceSeguimiento,
      LocalDateTime fecha) {
    this.setPersona(persona);
    this.setEntidadBeneficiaria(entidadBeneficiaria);
    this.setDetalleDonacion(detalleDonacion);
    this.setFecha(fecha);
    this.enlaceSeguimiento = enlaceSeguimiento;
  }

  public DonacionEnCamino() {}

  @Override
  protected String armarMensajeDonante() {
    return "El camión salió a entregar tu donación de "
        + getDetalleDonacion()
        + ". Seguí el recorrido en tiempo real: "
        + enlaceSeguimiento;
  }

  @Override
  protected String armarMensajeBeneficiario() {
    return "El camión con la donación de "
        + getDetalleDonacion()
        + " ya está en camino hacia tu sede. Seguí el recorrido en tiempo real: "
        + enlaceSeguimiento;
  }
}
