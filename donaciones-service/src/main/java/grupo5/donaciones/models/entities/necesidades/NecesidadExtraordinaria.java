package grupo5.donaciones.models.entities.necesidades;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NecesidadExtraordinaria extends Necesidad implements Asignable {
  private List<DonacionIndependiente> donacionesAsignadas;

  public NecesidadExtraordinaria(
      Subcategoria subcategoria, Integer cantidadNecesitada, String descripcion) {
    super(subcategoria, cantidadNecesitada, descripcion);
    this.donacionesAsignadas = new ArrayList<>();
  }

  @Override
  public void asignarDonacion(DonacionIndependiente donacion) {
    if (donacion == null) throw new ValidationException(ErrorCatalog.ASIGNAR_DONACION_NULA);
    if (this.donacionesAsignadas.contains(donacion))
      throw new ValidationException(ErrorCatalog.DONACION_YA_ASIGNADA);

    this.donacionesAsignadas.add(donacion);
  }

  @Override
  public void quitarDonacion(DonacionIndependiente donacion) {
    if (!this.donacionesAsignadas.contains(donacion))
      throw new ValidationException(ErrorCatalog.DONACION_NO_PERTENECE_A_NECESIDAD);

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

  @Override
  public Necesidad obtenerNecesidad() {
    return this;
  }
}
