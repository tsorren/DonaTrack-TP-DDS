package ar.edu.utn.frba.ddsi.donaciones.models.entities.beneficiarios;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.bienes.SubCategoria;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NecesidadExtraordinaria extends Necesidad {

  public NecesidadExtraordinaria(
      SubCategoria subcategoria, Integer cantidadNecesitada, String descripcion) {

    super(subcategoria, cantidadNecesitada, descripcion);
  }

  @Override
  public Integer cantidadAcumulada() {
    return this.donacionesAsignadas.stream().mapToInt(DonacionAsignada::getCantidad).sum();
  }

  @Override
  public boolean estaSatisfecha() {
    return this.cantidadAcumulada() >= this.getCantidadNecesitada();
  }
}
