package grupo5.donaciones.models.entities.beneficiarios;

import grupo5.donaciones.models.entities.bienes.SubCategoria;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Necesidad {
  private SubCategoria subcategoria;
  private Integer cantidadNecesitada;
  private String descripcion;
  private LocalDate fechaInicio;

  protected Necesidad(SubCategoria subcategoria, Integer cantidadNecesitada, String descripcion) {
    this.subcategoria = subcategoria;
    this.cantidadNecesitada = cantidadNecesitada;
    this.descripcion = descripcion;
    this.fechaInicio = LocalDate.now();

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

  public abstract void asignarDonacion(DonacionAsignada donacionAsignada);

  public abstract void quitarDonacion(DonacionAsignada donacionAsignada);

  public abstract boolean estaSatisfecha();

  public abstract Integer cantidadAcumulada();
}
