package grupo5.incentivos.models.entities.donante;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
public class EventoDonacion {

  private final UUID donacionId;
  private final List<String> categorias;
  private final Integer cantidadBienes;
  private final LocalDate fecha;

  @Builder
  public EventoDonacion(
      UUID donacionId, List<String> categorias, Integer cantidadBienes, LocalDate fecha) {
    if (fecha == null) {
      throw new ValidationException(ErrorCatalog.EVENTO_DONACION_SIN_FECHA);
    }
    if (cantidadBienes != null && cantidadBienes <= 0) {
      throw new ValidationException(ErrorCatalog.EVENTO_DONACION_CANTIDAD_INVALIDA);
    }
    this.donacionId = donacionId;
    this.categorias = categorias != null ? List.copyOf(categorias) : List.of();
    this.cantidadBienes = cantidadBienes;
    this.fecha = fecha;
  }
}
