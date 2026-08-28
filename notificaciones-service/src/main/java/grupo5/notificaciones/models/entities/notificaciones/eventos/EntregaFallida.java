package grupo5.notificaciones.models.entities.notificaciones.eventos;

import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.personas.Persona;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;

@Getter
public class EntregaFallida extends EventoDeDonacion {
  private static final String SEPARADOR_MOTIVO = ". Motivo: ";
  private final Persona administracion;
  private final String motivo;
  private final boolean replanificable;

  public EntregaFallida(
      Persona persona,
      Persona entidadBeneficiaria,
      Persona administracion,
      String detalleDonacion,
      String motivo,
      boolean replanificable,
      LocalDateTime fecha) {
    super(persona, entidadBeneficiaria, detalleDonacion, fecha);
    this.administracion = administracion;
    this.motivo = motivo;
    this.replanificable = replanificable;
  }

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
        + SEPARADOR_MOTIVO
        + motivo
        + "."
        + (replanificable ? " Vamos a reprogramar la entrega." : "");
  }

  @Override
  protected String armarMensajeBeneficiario() {
    return "No pudimos entregarte la donación "
        + getDetalleDonacion()
        + SEPARADOR_MOTIVO
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
        + SEPARADOR_MOTIVO
        + motivo
        + ". Replanificada: "
        + (replanificable ? "sí" : "no");
  }
}
