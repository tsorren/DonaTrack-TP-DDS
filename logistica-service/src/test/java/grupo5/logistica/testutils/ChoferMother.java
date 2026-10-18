package grupo5.logistica.testutils;

import grupo5.logistica.models.entities.choferes.Chofer;
import java.util.concurrent.atomic.AtomicInteger;

public final class ChoferMother {

  private static final AtomicInteger SECUENCIA = new AtomicInteger();

  private ChoferMother() {}

  public static Chofer disponible() {
    int numero = SECUENCIA.getAndIncrement();
    return new Chofer("Ada", "Lovelace", "LIC-" + numero, "11-5555-" + numero);
  }
}
