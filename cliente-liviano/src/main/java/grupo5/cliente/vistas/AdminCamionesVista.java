package grupo5.cliente.vistas;

import java.util.List;

/**
 * Camiones del admin (RF-18): la flota, la fecha de los datos ({@code al}, "Al 15 de octubre de
 * 2026, 18:30.") y las cifras del período de rendimiento. Logística no da el viaje, el chofer ni el
 * rendimiento: sin ellos, {@code entregas} y {@code periodo} son null y la pantalla no los muestra.
 */
public record AdminCamionesVista(
    List<Camion> camiones, String al, Integer entregas, String periodo) {

  public AdminCamionesVista {
    camiones = List.copyOf(camiones);
  }

  /**
   * Un camión. {@code clave} va en la dirección y en los formularios; {@code estado} es {@code
   * ruta}, {@code disponible} o {@code deshabilitado}; {@code tamano}, {@code chico}, {@code
   * mediano} o {@code grande}; {@code forma}, el dibujo ({@code camion} o {@code furgon}). {@code
   * nuevo}: recién agregado, todavía sin rutas.
   */
  public record Camion(
      String clave,
      String patente,
      String nombre,
      String forma,
      String capacidad,
      Integer kg,
      String tamano,
      String estado,
      String ahora,
      String motivo,
      String ultima,
      Chofer chofer,
      Viaje viaje,
      Rendimiento rendimiento,
      boolean nuevo) {}

  /** Quién maneja: la ruta de hoy, o la última. */
  public record Chofer(
      String nombre, String iniciales, String tel, String telTexto, String correo) {}

  /** La ruta en curso: etapa, recorrido, posición, próxima parada y carga. */
  public record Viaje(
      Integer etapa,
      Integer km,
      Integer kmTotal,
      String salio,
      String donde,
      String hacia,
      Integer paradas,
      Integer hechas,
      Double lat,
      Double lng,
      Double dLat,
      Double dLng,
      Integer carga,
      String cargaKg,
      String cargaM3,
      Integer bultos,
      List<String> cats) {

    public Viaje {
      cats = List.copyOf(cats);
    }
  }

  /**
   * Las cifras del período: entregas completas, a tiempo, demora, problemas y una barra por día.
   */
  public record Rendimiento(
      Integer entregas, String aTiempo, String demora, Integer problemas, List<Integer> barras) {

    public Rendimiento {
      barras = List.copyOf(barras);
    }
  }
}
