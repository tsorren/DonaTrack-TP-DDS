package grupo5.incentivos.fixtures;

import grupo5.incentivos.models.entities.donante.EventoDonacion;
import java.time.LocalDate;
import java.util.List;

public final class EventoDonacionMother {

  private EventoDonacionMother() {}

  public static EventoDonacion valido() {
    return enFecha(LocalDate.now());
  }

  public static EventoDonacion enFecha(LocalDate fecha) {
    return EventoDonacion.builder()
        .fecha(fecha)
        .cantidadBienes(5)
        .categorias(List.of("alimentos"))
        .build();
  }

  public static EventoDonacion enFecha(int anio, int mes, int dia) {
    return enFecha(LocalDate.of(anio, mes, dia));
  }

  public static EventoDonacion conCategorias(LocalDate fecha, List<String> categorias) {
    return EventoDonacion.builder().fecha(fecha).cantidadBienes(5).categorias(categorias).build();
  }

  public static EventoDonacion conCantidadBienes(LocalDate fecha, int cantidadBienes) {
    return EventoDonacion.builder()
        .fecha(fecha)
        .cantidadBienes(cantidadBienes)
        .categorias(List.of("alimentos"))
        .build();
  }
}
