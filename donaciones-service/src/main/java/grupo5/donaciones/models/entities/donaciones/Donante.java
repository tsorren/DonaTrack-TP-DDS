package grupo5.donaciones.models.entities.donaciones;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.donaciones.segmentaciones.DonacionSegmentada;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.privacidad.Anonimizable;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Donante implements Anonimizable {
  private Persona persona;
  private List<DonacionSegmentada> historialDonaciones = new ArrayList<>();

  public Donante(Persona persona) {

    if (persona == null) {
      throw new ValidationException(ErrorCatalog.DONANTE_SIN_PERSONA);
    }
    this.persona = persona;
  }

  public void agregarDonacion(DonacionSegmentada donacionSegmentada) {
    if (donacionSegmentada == null) {
      throw new ValidationException(ErrorCatalog.DONANTE_DONACION_NULA);
    }

    if (this.historialDonaciones.contains(donacionSegmentada)) {
      throw new ValidationException(ErrorCatalog.DONANTE_DONACION_YA_HISTORIAL);
    }
    this.historialDonaciones.add(donacionSegmentada);
  }

  // Lanzar excepcion si la donacion no esta en la lista
  public void quitarDonacion(DonacionSegmentada donacionSegmentada) {
    if (!this.historialDonaciones.contains(donacionSegmentada)) {
      throw new ValidationException(ErrorCatalog.DONANTE_DONACION_NO_HISTORIAL);
    }
    this.historialDonaciones.remove(donacionSegmentada);
  }

  @Override
  public void anonimizar() {
    this.persona.anonimizar();
  }
}
