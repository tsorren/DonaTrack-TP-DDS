package grupo5.logistica.testutils;

import grupo5.logistica.models.entities.camiones.Camion;
import java.util.concurrent.atomic.AtomicInteger;

public final class CamionMother {

  private static final AtomicInteger SECUENCIA = new AtomicInteger();

  private CamionMother() {}

  public static Camion disponible() {
    return conCapacidad(20f, 5000f, 3f);
  }

  public static Camion conCapacidad(float volumenM3, float pesoKg, float alturaM) {
    int numero = Math.floorMod(SECUENCIA.getAndIncrement(), 1000);
    return new Camion("AB%03dCD".formatted(numero), volumenM3, pesoKg, alturaM);
  }
}
