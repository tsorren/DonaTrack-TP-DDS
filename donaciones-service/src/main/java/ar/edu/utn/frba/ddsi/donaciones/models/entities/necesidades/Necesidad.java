package ar.edu.utn.frba.ddsi.donaciones.models.entities.necesidades;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Subcategoria;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public abstract class Necesidad {
  private Subcategoria subcategoria;
  private String descripcion;
  protected List<DonacionAsignada> donacionesAsignadas;

  public abstract boolean estaSatisfecha();
}
