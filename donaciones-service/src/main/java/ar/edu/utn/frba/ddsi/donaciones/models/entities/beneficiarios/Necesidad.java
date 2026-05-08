package ar.edu.utn.frba.ddsi.donaciones.models.entities.beneficiarios;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.bienes.SubCategoria;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
public abstract class Necesidad {
  private SubCategoria subcategoria;
  private Integer cantidadNecesitada;
  private String descripcion;
  protected List<DonacionAsignada> donacionesAsignadas = new ArrayList<>();

  public void asignarDonacion(DonacionAsignada donacionAsignada) {
    this.donacionesAsignadas.add(donacionAsignada);
  }

  // Añadir excepcion si no está el elemento en la lista
  public void quitarDonacion(DonacionAsignada donacionAsignada) {
    this.donacionesAsignadas.remove(donacionAsignada);
  }

  public boolean estaSatisfecha() {
    return this.cantidadAcumulada() >= this.cantidadNecesitada;
  }

  public abstract Integer cantidadAcumulada();
}
