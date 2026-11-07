package grupo5.tests.builders;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Payloads de Logística para crear recursos de prueba. */
public final class LogisticaTestData {
  private LogisticaTestData() {}

  public static Map<String, Object> camion() {
    return Map.of(
        "patente", "AB" + ThreadLocalRandom.current().nextInt(100, 999) + "CD",
        "capacidadVolumen", 45.0f,
        "altura", 2.8f,
        "capacidadKG", 3500.0f);
  }

  public static Map<String, Object> chofer() {
    return Map.of(
        "nombre",
        "Roberto",
        "apellido",
        "Gomez",
        "licencia",
        "LIC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
        "telefonoContacto",
        "+5411" + ThreadLocalRandom.current().nextInt(10000000, 99999999));
  }

  /** Entrega sin donación detrás: Donaciones descarta los eventos que la mencionan. */
  public static Map<String, Object> entregaSinDonacion() {
    return Map.of(
        "idDonacion", UUID.randomUUID(),
        "idBeneficiaria", UUID.randomUUID(),
        "destino",
            Map.of(
                "calle", "Av. San Martin",
                "altura", 1234,
                "codigoPostal", "1416",
                "localidad", "CABA",
                "provincia", "Buenos Aires",
                "pais", "Argentina"),
        "pesoTotalKG", 20.0f,
        "volumenTotalM3", 2.5f);
  }
}
