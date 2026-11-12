package grupo5.cliente.vistas;

import java.util.List;

/**
 * Inicio del admin (RF-16, RF-17, RF-21 y RF-18, resumidos), por temas (R11-21): lo que espera una
 * decisión arriba y cómo está todo en cuatro tarjetas. {@code fecha} es el día de hoy ("Jueves 15
 * de octubre de 2026"); {@code resumen}, la frase de la cabecera, ya con separador de miles, o null
 * si no hay nada que decidir; {@code al}, cuándo se calcularon las cifras.
 */
public record AdminInicioVista(
    String fecha, String resumen, Decisiones decisiones, String al, Cifras cifras) {

  /**
   * Lo que espera una decisión: las organizaciones que piden sumarse (como mucho 3 nombres y
   * cuántas más hay), las donaciones sin asignar en el depósito y cuántas vencen en los próximos 7
   * días (con la primera, si se sabe), y las propuestas de asignación pendientes (solo el número).
   */
  public record Decisiones(
      long entidades,
      List<String> nombresEntidades,
      long entidadesDeMas,
      long sinAsignar,
      long vencen,
      String primeraQueVence,
      long propuestas) {

    /** Si no hay nada que decidir. */
    public boolean nada() {
      return entidades == 0 && sinAsignar == 0 && propuestas == 0;
    }
  }

  /**
   * Cómo está todo. Donaciones por estado, en las 5 filas de la lista del admin: en el depósito,
   * asignadas por salir, en camino o en la puerta, las que no se pudieron entregar y las vencidas.
   * Entregas: las del mes ({@code delMes}: "en octubre, del 1 al 15") y el total. La red: las
   * organizaciones activas y los donantes activos de los registrados. {@code camiones} es null si
   * Logística no respondió: esa tarjeta muestra su aviso y el resto se ve igual.
   */
  public record Cifras(
      long enDeposito,
      long asignadas,
      long enCamino,
      long fallidas,
      long vencidas,
      long entregadasEnElMes,
      String delMes,
      long entregadasEnTotal,
      long entidadesActivas,
      long donantesActivos,
      long donantesTotal,
      Camiones camiones) {}

  /** La flota, ahora: en ruta y libres. */
  public record Camiones(long enRuta, long libres) {}
}
