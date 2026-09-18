package grupo5.donaciones.fixtures;

import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.propuestas.PosibleFragmentacion;
import grupo5.donaciones.models.entities.propuestas.Propuesta;
import java.util.UUID;

public final class PropuestaMother {

  private PropuestaMother() {}

  public static Propuesta simple(UUID necesidadId, DonacionIndependiente donacion, int cantidad) {
    Propuesta p = new Propuesta();
    p.asociarNecesidad(necesidadId);
    p.agregarFragmentacion(donacion, cantidad);
    return p;
  }

  public static PosibleFragmentacion fragmentacion(DonacionIndependiente donacion, int cantidad) {
    return new PosibleFragmentacion(donacion, cantidad);
  }
}
