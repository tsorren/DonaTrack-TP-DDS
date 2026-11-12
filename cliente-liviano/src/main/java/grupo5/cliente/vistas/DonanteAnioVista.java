package grupo5.cliente.vistas;

import grupo5.cliente.vistas.DonanteInicioVista.Aviso;
import java.util.List;

/**
 * El inicio de la persona que dona: su año mes a mes (docs/interfaz/flujos/r11-p0-persona.md). Las
 * cifras llegan como números, contadas por Donaciones, y de cada mes solo las {@link #POR_MES} más
 * recientes (regla 2 del backlog R11: un resumen muestra 3 y "Ver todas").
 *
 * @param anio el año de hoy
 * @param mesHoy el mes de hoy, de 1 a 12
 * @param meses de enero al mes de hoy, en ese orden
 * @param total las donaciones del año
 * @param entregadas las del año con la entrega confirmada
 * @param enCamino las del año que van a la entidad
 * @param enDeposito las del año que esperan en el depósito
 * @param algunaVez si la cuenta tiene alguna donación, de este año o de antes
 * @param avisos las noticias más nuevas de las donaciones que se muestran
 * @param sinLeer las notificaciones sin leer de la cuenta
 * @param hoy "15 de octubre"
 * @param actualizado "15 de octubre, 16:25"
 * @param hoyIso y {@code ayerIso}: los días de "Hoy" y "Ayer" de las noticias ("2026-10-15")
 */
public record DonanteAnioVista(
    int anio,
    int mesHoy,
    List<Mes> meses,
    long total,
    long entregadas,
    long enCamino,
    long enDeposito,
    boolean algunaVez,
    List<Aviso> avisos,
    long sinLeer,
    String hoy,
    String actualizado,
    String hoyIso,
    String ayerIso) {

  /** Cuántas donaciones de cada mes muestra el inicio. */
  public static final int POR_MES = 3;

  /**
   * Un mes del año.
   *
   * @param numero de 1 a 12
   * @param nombre "octubre"
   * @param clave "2026-10", el filtro de Mis donaciones para "Ver todas"
   * @param total las donaciones del mes, contadas por Donaciones
   * @param primeras las más recientes, hasta {@link #POR_MES}
   */
  public record Mes(
      int numero, String nombre, String clave, long total, List<DonanteDonacionVista> primeras) {}
}
