package grupo5.donaciones.models.entities.necesidades;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import lombok.Getter;

@Getter
public class NecesidadExtraordinaria extends Necesidad {

  private boolean activa;

  /**
   * Hasta cuándo sirve. Obligatoria y futura al crearla; nula solo en las creadas antes de exigirla
   * (ADR 20261102 del ciclo de vida, punto 3).
   */
  private final LocalDate fechaLimite;

  public NecesidadExtraordinaria(
      UUID subcategoriaId, Integer cantidadNecesitada, String descripcion, LocalDate fechaLimite) {
    super(subcategoriaId, cantidadNecesitada, descripcion);
    if (fechaLimite == null) {
      throw new ValidationException(ErrorCatalog.FECHA_LIMITE_NULA);
    }
    if (!fechaLimite.isAfter(LocalDate.now(ZoneId.systemDefault()))) {
      throw new ValidationException(ErrorCatalog.FECHA_LIMITE_NO_FUTURA);
    }
    this.fechaLimite = fechaLimite;
    this.activa = true;
  }

  /** Constructor de reconstitución para el adaptador JPA */
  public NecesidadExtraordinaria(DatosNecesidad datos, boolean activa, LocalDate fechaLimite) {
    super(datos);
    this.activa = activa;
    this.fechaLimite = fechaLimite;
  }

  @Override
  public TipoNecesidad getTipoNecesidad() {
    return TipoNecesidad.EXTRAORDINARIA;
  }

  @Override
  public EstadoNecesidad estado(int recibido, LocalDate hoy) {
    if (!activa) {
      return EstadoNecesidad.DADA_DE_BAJA;
    }
    if (recibido >= getCantidadNecesitada()) {
      return EstadoNecesidad.CUMPLIDA;
    }
    return vencida(hoy) ? EstadoNecesidad.TERMINADA : EstadoNecesidad.VIGENTE;
  }

  @Override
  public boolean admiteAsignaciones(LocalDate hoy) {
    return activa && !vencida(hoy);
  }

  private boolean vencida(LocalDate hoy) {
    return fechaLimite != null && hoy.isAfter(fechaLimite);
  }

  @Override
  public DestinoAsignacion destinoDeAsignacion() {
    return new DestinoAsignacion(getId(), null, getEntidadId());
  }

  @Override
  public UUID periodoActualId() {
    return null;
  }

  /** Se satisface con las donaciones parciales que igualan o superan lo pedido. */
  @Override
  public boolean estaSatisfechaCon(int cantidadAcumulada) {
    return cantidadAcumulada >= this.getCantidadNecesitada();
  }

  @Override
  public boolean isActiva() {
    return this.activa;
  }

  @Override
  public void desactivar() {
    this.activa = false;
  }
}
