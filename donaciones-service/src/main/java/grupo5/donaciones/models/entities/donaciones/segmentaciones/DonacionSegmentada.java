package grupo5.donaciones.models.entities.donaciones.segmentaciones;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.bienes.SubCategoria;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DonacionSegmentada {
  private List<DonacionIndependiente> donacionesIndependientes;
  private Donacion donacion;

  public DonacionSegmentada(
      Donacion donacion, List<DonacionIndependiente> donacionesIndependientes) {
    if (donacion == null) {
      throw new ValidationException(ErrorCatalog.DONACION_SEGMENTADA_SIN_DONACION);
    }

    this.donacionesIndependientes = new ArrayList<>();

    this.donacion = donacion;

    donacionesIndependientes.forEach(this::agregarDonacionIndependiente);
  }

  public void agregarDonacionIndependiente(DonacionIndependiente donacionIndependiente) {
    if (donacionIndependiente == null) {
      throw new ValidationException(ErrorCatalog.DONACION_SEGMENTADA_AGREGAR_INDEPENDIENTE_NULA);
    }

    if (this.donacionesIndependientes.contains(donacionIndependiente)) {
      throw new ValidationException(
          ErrorCatalog.DONACION_SEGMENTADA_AGREGAR_INDEPENDIENTE_REPETIDA);
    }

    donacionIndependiente.setDonacionSegmentada(this);

    this.donacionesIndependientes.add(donacionIndependiente);
  }

  // Lanzar excepcion si la donacion independiente no esta en la lista
  public void quitarDonacionIndependiente(DonacionIndependiente donacionIndependiente) {
    if (!this.donacionesIndependientes.contains(donacionIndependiente)) {
      throw new ValidationException(
          ErrorCatalog.DONACION_SEGMENTADA_QUITAR_INDEPENDIENTE_INEXISTENTE);
    }

    this.donacionesIndependientes.remove(donacionIndependiente);
  }

  public Integer cantidadDisponible(SubCategoria subCategoria) {
    return this.donacionesIndependientes.stream()
        .filter(d -> d.getSubCategoria().equals(subCategoria))
        .mapToInt(DonacionIndependiente::getCantidad)
        .sum();
  }
}
