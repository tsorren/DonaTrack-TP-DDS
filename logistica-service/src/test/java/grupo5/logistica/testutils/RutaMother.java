package grupo5.logistica.testutils;

import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.rutas.Ruta;
import java.time.LocalDate;

public final class RutaMother {

  private RutaMother() {}

  public static Ruta pendiente() {
    return new Ruta(
        LocalDate.now().plusDays(1),
        ChoferMother.disponible().getId(),
        CamionMother.disponible().getId());
  }

  public static Ruta pendienteCon(Entrega entrega) {
    Ruta ruta = pendiente();
    ruta.agregarEntrega(entrega.getId());
    entrega.asignarRuta(ruta.getId());
    return ruta;
  }
}
