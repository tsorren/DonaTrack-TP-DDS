package grupo5.donaciones.models.entities.beneficiarios;

import grupo5.donaciones.models.entities.bienes.SubCategoria;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Necesidad {
  protected List<DonacionAsignada> donacionesAsignadas;
  private SubCategoria subcategoria;
  private Integer cantidadNecesitada;
  private String descripcion;

  public Necesidad(SubCategoria subcategoria, Integer cantidadNecesitada, String descripcion) {
    this.subcategoria = subcategoria;
    this.cantidadNecesitada = cantidadNecesitada;
    this.descripcion = descripcion;

    this.donacionesAsignadas = new ArrayList<>();

    validarNecesidad();
  }

  private void validarNecesidad() {

    if (this.subcategoria == null) {
      throw new IllegalArgumentException("La necesidad debe tener una subcategoría.");
    }

    if (this.cantidadNecesitada == null || this.cantidadNecesitada <= 0) {
      throw new IllegalArgumentException("La cantidad necesitada debe ser mayor a cero.");
    }

    if (this.descripcion == null || this.descripcion.trim().isEmpty()) {
      throw new IllegalArgumentException("La descripción de la necesidad no puede estar vacía.");
    }
  }

  public void asignarDonacion(DonacionAsignada donacionAsignada) {
    if (donacionAsignada == null) {
      throw new IllegalArgumentException("La donación asignada no puede ser nula.");
    }

    if (this.donacionesAsignadas.contains(donacionAsignada)) {
      throw new IllegalArgumentException("La donación ya fue asignada a esta necesidad.");
    }

    this.donacionesAsignadas.add(donacionAsignada);
  }

  // Añadir excepcion si no está el elemento en la lista
  public void quitarDonacion(DonacionAsignada donacionAsignada) {
    if (!this.donacionesAsignadas.contains(donacionAsignada)) {
      throw new IllegalArgumentException("La donación no pertenece a esta necesidad.");
    }

    this.donacionesAsignadas.remove(donacionAsignada);
  }

  public boolean estaSatisfecha() {
    return this.cantidadAcumulada() >= this.cantidadNecesitada;
  }

  public abstract Integer cantidadAcumulada();
}
