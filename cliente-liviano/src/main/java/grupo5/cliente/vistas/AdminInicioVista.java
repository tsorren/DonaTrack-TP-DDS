package grupo5.cliente.vistas;

import java.util.List;

/**
 * Inicio del admin (RF-16, RF-17, RF-21 y RF-18, resumidos): lo que espera una decisión y cómo está
 * todo. {@code fecha} es el día de hoy ("Jueves 15 de octubre de 2026"); {@code resumen}, la frase
 * de la cabecera; {@code al}, cuándo se leyeron las cifras. Las frases que no se pueden armar con
 * lo que dan las APIs ({@code notaEntidades}, {@code primeraQueVence}) quedan null.
 */
public record AdminInicioVista(
    String fecha, String resumen, Decisiones decisiones, String al, Cifras cifras) {

  /**
   * Lo que espera una decisión: las entidades que piden sumarse (sus nombres), las donaciones que
   * vencen en los próximos 7 días y las propuestas de asignación (cada una en una frase).
   */
  public record Decisiones(
      int entidades,
      List<String> nombresEntidades,
      String notaEntidades,
      int vencen,
      String primeraQueVence,
      int propuestas,
      List<String> listaPropuestas) {

    /** Si no hay nada que decidir. */
    public boolean nada() {
      return entidades == 0 && vencen == 0 && propuestas == 0;
    }
  }

  /** Cómo está todo; {@code mes} es el de las entregas confirmadas ("octubre"). */
  public record Cifras(
      long enDeposito,
      long asignadas,
      long enCamino,
      long entregadasEnElMes,
      String mes,
      long entidadesActivas,
      long donantes,
      long camionesEnRuta,
      long camionesLibres) {}
}
