package grupo5.donaciones.models.entities.donaciones.matchmaking.propuestas;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.RecursoDTO;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Propuesta implements RecursoDTO {
  UUID id;
  Necesidad necesidadQueSatisface;
  List<PosibleFragmentacion> posiblesFragmentaciones;
  EstadoPropuesta estado;
  LocalDateTime fechaCreacion;

  public void agregarFragmentacion(DonacionIndependiente donacion, int cantidad) {
    if (donacion == null)
      throw new ValidationException(ErrorCatalog.PROPUESTA_FRAGMENTACION_DONACION_NULA);
    if (cantidad <= 0)
      throw new ValidationException(ErrorCatalog.PROPUESTA_FRAGMENTACION_CANTIDAD_INVALIDA);
    if (posiblesFragmentaciones == null) posiblesFragmentaciones = new ArrayList<>();
    PosibleFragmentacion f = new PosibleFragmentacion();
    f.setDonacionOriginal(donacion);
    f.setCantidadNecesaria(cantidad);
    posiblesFragmentaciones.add(f);
  }

  public boolean estaActiva() {
    return this.estado != null && this.estado != EstadoPropuesta.DESCARTADA;
  }

  public void confirmar() {
    confirmar("SISTEMA");
  }

  public void confirmar(String actor) {
    if (necesidadQueSatisface == null) {
      throw new ValidationException(ErrorCatalog.PROPUESTA_CONFIRMAR_SIN_NECESIDAD);
    }

    posiblesFragmentaciones.forEach(f -> f.confirmar(necesidadQueSatisface, actor));
    this.estado = EstadoPropuesta.APROBADA;
  }

  public void rechazar() {
    this.estado = EstadoPropuesta.DESCARTADA;
  }
}
