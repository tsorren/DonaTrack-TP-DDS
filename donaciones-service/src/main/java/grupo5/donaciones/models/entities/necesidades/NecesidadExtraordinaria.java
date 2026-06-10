package grupo5.donaciones.models.entities.necesidades;

import grupo5.donaciones.models.entities.categorias.SubCategoria;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NecesidadExtraordinaria extends Necesidad {
  private List<DonacionIndependiente> donacionesAsignadas;

  public NecesidadExtraordinaria(
      SubCategoria subcategoria, Integer cantidadNecesitada, String descripcion) {
    super(subcategoria, cantidadNecesitada, descripcion);
    this.donacionesAsignadas = new ArrayList<>();
  }

  @Override
  public void asignarDonacion(DonacionIndependiente donacion) {
    if (donacion == null) throw new IllegalArgumentException("La donación no puede ser nula.");
    if (this.donacionesAsignadas.contains(donacion))
      throw new IllegalArgumentException("La donación ya fue asignada.");

    this.donacionesAsignadas.add(donacion);
  }

  @Override
  public void quitarDonacion(DonacionIndependiente donacion) {
    if (!this.donacionesAsignadas.contains(donacion))
      throw new IllegalArgumentException("La donación no pertenece a esta necesidad.");

    this.donacionesAsignadas.remove(donacion);
  }

  @Override
  public Integer cantidadAcumulada() {
    return this.donacionesAsignadas.stream().mapToInt(DonacionIndependiente::getCantidad).sum();
  }

  @Override
  public boolean estaSatisfecha() {
    return this.cantidadAcumulada() >= this.getCantidadNecesitada();
  }
}
