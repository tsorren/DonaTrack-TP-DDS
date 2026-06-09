package grupo5.donaciones.models.entities.beneficiarios;

import grupo5.donaciones.models.entities.bienes.SubCategoria;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NecesidadExtraordinaria extends Necesidad {
  private List<DonacionAsignada> donacionesAsignadas;

  public NecesidadExtraordinaria(
      SubCategoria subcategoria, Integer cantidadNecesitada, String descripcion) {
    super(subcategoria, cantidadNecesitada, descripcion);
    this.donacionesAsignadas = new ArrayList<>();
  }

  @Override
  public void asignarDonacion(DonacionAsignada donacion) {
    if (donacion == null) throw new IllegalArgumentException("La donación no puede ser nula.");
    if (this.donacionesAsignadas.contains(donacion))
      throw new IllegalArgumentException("La donación ya fue asignada.");

    this.donacionesAsignadas.add(donacion);
  }

  @Override
  public void quitarDonacion(DonacionAsignada donacion) {
    if (!this.donacionesAsignadas.contains(donacion))
      throw new IllegalArgumentException("La donación no pertenece a esta necesidad.");

    this.donacionesAsignadas.remove(donacion);
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
