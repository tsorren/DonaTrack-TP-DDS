package grupo5.donaciones.fixtures;

import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.Deposito;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.ItemDonacion;
import java.time.LocalDateTime;
import java.util.UUID;

public final class DonacionMother {

  private DonacionMother() {}

  public static Deposito depositoCentral() {
    return new Deposito("Depósito Central", PersonaMother.direccionValida());
  }

  public static Donacion simple(UUID donanteId) {
    return new Donacion(donanteId, depositoCentral(), "Donación de prueba", LocalDateTime.now());
  }

  public static Donacion conItem(UUID donanteId, Bien bien, int cantidad) {
    Donacion d = simple(donanteId);
    d.agregarItem(new ItemDonacion(bien, cantidad));
    return d;
  }

  public static Donacion normalizada(UUID donanteId, Bien bien, int cantidad) {
    Donacion d = conItem(donanteId, bien, cantidad);
    d.marcarNormalizada();
    return d;
  }

  public static Donacion segmentada(UUID donanteId, Bien bien, int cantidad) {
    Donacion d = normalizada(donanteId, bien, cantidad);
    d.marcarSegmentada();
    return d;
  }
}
