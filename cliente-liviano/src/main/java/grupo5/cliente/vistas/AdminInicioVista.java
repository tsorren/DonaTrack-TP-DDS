package grupo5.cliente.vistas;

import java.util.List;

/**
 * Inicio del admin (RF-16, RF-17, RF-21 y RF-18, resumidos), por temas (R11-21): lo que espera una
 * decisión arriba y cómo está todo en cuatro tarjetas y, debajo, los gráficos de los últimos 12
 * meses (R11-42). {@code fecha} es el día de hoy ("Jueves 15 de octubre de 2026"); {@code resumen},
 * la frase de la cabecera, ya con separador de miles, o null si no hay nada que decidir; {@code
 * al}, cuándo se calcularon las cifras; {@code evolucion}, los gráficos, o null si Donaciones no
 * los dio (solo esa tarjeta muestra su aviso).
 */
public record AdminInicioVista(
    String fecha,
    String resumen,
    Decisiones decisiones,
    String al,
    Cifras cifras,
    Evolucion evolucion) {

  /** Sin los gráficos: la tarjeta de los gráficos muestra su aviso. */
  public AdminInicioVista(
      String fecha, String resumen, Decisiones decisiones, String al, Cifras cifras) {
    this(fecha, resumen, decisiones, al, cifras, null);
  }

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

  /**
   * Los gráficos del inicio (R11-42), ya con sus textos. {@code periodo}: "de noviembre de 2025 a
   * octubre de 2026"; {@code frase} y {@code fraseCategorias}: lo principal de cada gráfico en
   * palabras; {@code nota}: por qué un mes puede tener más entregas que registradas y si el último
   * no terminó; {@code tope} y {@code mitad}: las líneas de guía de la escala (la de abajo es 0).
   * Sin meses, todavía no hay donaciones: la tarjeta lo dice y no dibuja nada.
   */
  public record Evolucion(
      String periodo,
      String frase,
      String nota,
      long tope,
      long mitad,
      List<Mes> meses,
      Fila total,
      String fraseCategorias,
      List<Fila> categorias,
      Fila totalCategorias) {

    /** Todavía no hay donaciones registradas. */
    public static final Evolucion VACIA =
        new Evolucion(null, null, null, 0, 0, List.of(), null, null, List.of(), null);

    public boolean vacia() {
      return meses.isEmpty();
    }
  }

  /**
   * Un mes: {@code corto} ("nov"), {@code anio} (solo debajo del primero y de enero, si no null),
   * {@code nombre} para la tabla ("Noviembre 2025" u "Octubre 2026, del 1 al 15"), si es el mes en
   * curso, sus cifras y el alto de cada columna en centésimos del tope.
   */
  public record Mes(
      String corto,
      String anio,
      String nombre,
      boolean enCurso,
      long registradas,
      long entregadas,
      int altoRegistradas,
      int altoEntregadas) {}

  /**
   * Una categoría (o "Otras 3 categorías", o el total): su nombre, su ícono ({@code cat-…}), sus
   * cifras y el largo de cada barra en centésimos de la mayor.
   */
  public record Fila(
      String nombre,
      String icono,
      long registradas,
      long entregadas,
      int largoRegistradas,
      int largoEntregadas) {}
}
