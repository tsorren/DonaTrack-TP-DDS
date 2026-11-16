package grupo5.cliente.vistas;

import java.util.List;

/**
 * El inicio del donante: sus donaciones (la pantalla las separa en el tablero), las últimas
 * noticias y las fechas con que se leen las cifras.
 *
 * @param avisos las noticias más nuevas de sus donaciones, de la más nueva a la más vieja
 * @param sinLeer las notificaciones sin leer de la cuenta
 * @param entregadasDelMes las entregas confirmadas en el mes de {@code mes}
 * @param mes "octubre de 2026"
 * @param hoy "15 de octubre"
 * @param actualizado "15 de octubre, 16:25"
 * @param desde el día de la primera donación ("1 de octubre")
 * @param hoyIso y {@code ayerIso}: los días de "Hoy" y "Ayer" de las noticias ("2026-10-15")
 */
public record DonanteInicioVista(
    List<DonanteDonacionVista> donaciones,
    List<Aviso> avisos,
    long sinLeer,
    int entregadasDelMes,
    String mes,
    String hoy,
    String actualizado,
    String desde,
    String hoyIso,
    String ayerIso) {

  /** Una noticia: la carta {@code i} de la donación {@code d}, como la lee datos-donante. */
  public record Aviso(DonanteDonacionVista d, int i) {}

  /** "Octubre de 2026", para el rótulo de las cifras. */
  public String mesConMayuscula() {
    return mes == null || mes.isEmpty()
        ? mes
        : Character.toUpperCase(mes.charAt(0)) + mes.substring(1);
  }

  /** "octubre", sin el año. */
  public String nombreDelMes() {
    return mes == null ? null : mes.split(" ")[0];
  }
}
