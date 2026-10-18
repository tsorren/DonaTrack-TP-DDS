package grupo5.logistica.testutils;

import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.rutas.direccion.Direccion;
import grupo5.logistica.models.entities.rutas.direccion.Localidad;
import grupo5.logistica.models.entities.rutas.direccion.Pais;
import grupo5.logistica.models.entities.rutas.direccion.Provincia;
import java.util.UUID;

public final class EntregaMother {

  private EntregaMother() {}

  public static Entrega pendiente() {
    return conDimensiones(10f, 2f);
  }

  public static Entrega conDimensiones(float pesoKg, float volumenM3) {
    return new Entrega(UUID.randomUUID(), UUID.randomUUID(), direccion(), pesoKg, volumenM3);
  }

  public static Direccion direccion() {
    return new Direccion(
        "Calle Falsa",
        123,
        4,
        "B",
        "C1000",
        new Localidad("CABA", new Provincia("Buenos Aires", new Pais("Argentina"))));
  }
}
