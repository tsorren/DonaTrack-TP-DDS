package grupo5.logistica.testutils;

import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.entities.rutas.direccion.CoordenadasGeograficas;
import grupo5.logistica.models.entities.rutas.direccion.Direccion;
import grupo5.logistica.models.entities.rutas.direccion.Localidad;
import grupo5.logistica.models.entities.rutas.direccion.Pais;
import grupo5.logistica.models.entities.rutas.direccion.Provincia;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

public final class EntregaMother {

  private EntregaMother() {}

  public static Entrega pendiente() {
    return conDimensiones(10f, 2f);
  }

  public static Entrega pendienteEn(CoordenadasGeograficas coordenadas) {
    return new Entrega(UUID.randomUUID(), UUID.randomUUID(), direccionEn(coordenadas), 10f, 2f);
  }

  public static Entrega conDimensiones(float pesoKg, float volumenM3) {
    return new Entrega(UUID.randomUUID(), UUID.randomUUID(), direccion(), pesoKg, volumenM3);
  }

  /** Una entrega ENTREGADA (reconstituida) que llegó en {@code arribo}, con o sin coordenadas. */
  public static Entrega entregadaEl(Instant arribo, CoordenadasGeograficas coordenadas) {
    return enEstadoEl(EstadoEntrega.ENTREGADA, arribo, coordenadas);
  }

  public static Entrega enEstadoEl(
      EstadoEntrega estado, Instant arribo, CoordenadasGeograficas coordenadas) {
    return new Entrega(
        UUID.randomUUID(),
        null,
        UUID.randomUUID(),
        UUID.randomUUID(),
        direccionEn(coordenadas),
        estado,
        List.of(),
        arribo == null ? null : LocalDateTime.ofInstant(arribo, ZoneOffset.UTC),
        null,
        List.of(),
        10f,
        2f,
        null);
  }

  public static Direccion direccion() {
    return direccionEn(null);
  }

  public static Direccion direccionEn(CoordenadasGeograficas coordenadas) {
    return new Direccion(
        "Calle Falsa",
        123,
        4,
        "B",
        "C1000",
        new Localidad("CABA", new Provincia("Buenos Aires", new Pais("Argentina"))),
        coordenadas);
  }
}
