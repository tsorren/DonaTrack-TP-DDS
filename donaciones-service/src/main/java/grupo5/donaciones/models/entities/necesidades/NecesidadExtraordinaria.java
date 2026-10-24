package grupo5.donaciones.models.entities.necesidades;

import grupo5.donaciones.dto.NecesidadDTO;
import java.util.UUID;
import lombok.Getter;

@Getter
public class NecesidadExtraordinaria extends Necesidad {

  private boolean activa;

  public NecesidadExtraordinaria(
      UUID subcategoriaId, Integer cantidadNecesitada, String descripcion) {
    super(subcategoriaId, cantidadNecesitada, descripcion);
    this.activa = true;
  }

  /** Constructor de reconstitución para el adaptador JPA */
  public NecesidadExtraordinaria(DatosNecesidad datos, boolean activa) {
    super(datos);
    this.activa = activa;
  }

  @Override
  public TipoNecesidad getTipoNecesidad() {
    return TipoNecesidad.EXTRAORDINARIA;
  }

  @Override
  public NecesidadDTO toDTO(int cantidadAcumulada) {
    return super.toDTO(null, estaSatisfechaCon(cantidadAcumulada));
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
