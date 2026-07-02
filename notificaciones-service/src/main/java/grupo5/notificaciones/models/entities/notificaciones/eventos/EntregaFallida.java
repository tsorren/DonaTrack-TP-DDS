package grupo5.notificaciones.models.entities.notificaciones.eventos;

import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.personas.Persona;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EntregaFallida extends EventoDeDonacion {

  private Persona administracion;
  private String motivo;
  private boolean replanificable;

  public EntregaFallida(
      Persona persona,
      Persona entidadBeneficiaria,
      Persona administracion,
      String detalleDonacion,
      String motivo,
      boolean replanificable,
      LocalDateTime fecha) {
    this.setPersona(persona);
    this.setEntidadBeneficiaria(entidadBeneficiaria);
    this.setDetalleDonacion(detalleDonacion);
    this.setFecha(fecha);
    this.administracion = administracion;
    this.motivo = motivo;
    this.replanificable = replanificable;
  }

  public EntregaFallida() {}

  @Override
  public List<Notificacion> generarNotificaciones() {
    Notificacion aDonante = new Notificacion(this.getPersona().getId(), this.armarMensajeDonante());

    Notificacion aBeneficiario =
        new Notificacion(this.getEntidadBeneficiaria().getId(), this.armarMensajeBeneficiario());

    Notificacion aAdmin = new Notificacion(this.administracion.getId(), this.armarMensajeAdmin());

    return List.of(aDonante, aBeneficiario, aAdmin);
  }

  @Override
  protected String armarMensajeDonante() {
    return "No pudimos entregar tu donación de "
        + getDetalleDonacion()
        + ". Motivo: "
        + motivo
        + "."
        + (replanificable ? " Vamos a reprogramar la entrega." : "");
  }

  @Override
  protected String armarMensajeBeneficiario() {
    return "No pudimos entregarte la donación "
        + getDetalleDonacion()
        + ". Motivo: "
        + motivo
        + "."
        + (replanificable ? " La entrega será reprogramada." : "");
  }

  private String armarMensajeAdmin() {
    return "Entrega fallida — donante: "
        + getPersona().getDenominacion()
        + ", entidad: "
        + getEntidadBeneficiaria().getDenominacion()
        + ", donación: "
        + getDetalleDonacion()
        + ". Motivo: "
        + motivo
        + ". Replanificada: "
        + (replanificable ? "sí" : "no");
  }
}
