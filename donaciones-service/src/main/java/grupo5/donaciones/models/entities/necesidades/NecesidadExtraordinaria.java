package grupo5.donaciones.models.entities.necesidades;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.NecesidadDTO;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;

@Getter
public class NecesidadExtraordinaria extends Necesidad implements Asignable {
  private List<DonacionIndependiente> donacionesAsignadas;
  private boolean activa;

  public NecesidadExtraordinaria(
      UUID subcategoriaId, Integer cantidadNecesitada, String descripcion) {
    super(subcategoriaId, cantidadNecesitada, descripcion);
    this.donacionesAsignadas = new ArrayList<>();
    this.activa = true;
  }

  @Override
  public TipoNecesidad getTipoNecesidad() {
    return TipoNecesidad.EXTRAORDINARIA;
  }

  @Override
  public NecesidadDTO toDTO() {
    return super.toDTO(null);
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
  public boolean isActiva() {
    return this.activa;
  }

  @Override
  public void desactivar() {
    this.activa = false;
  }

  @Override
  public int contarDonacionesAsignadasDesde(LocalDateTime desde) {
    if (this.donacionesAsignadas == null || desde == null) return 0;
    return (int)
        this.donacionesAsignadas.stream()
            .filter(d -> d.getFechaRegistro() != null && d.getFechaRegistro().isAfter(desde))
            .count();
  }

  @Override
  public Necesidad obtenerNecesidad() {
    return this;
  }
}
