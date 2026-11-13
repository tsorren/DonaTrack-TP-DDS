package grupo5.cliente.vistas;

import grupo5.cliente.vistas.AdminDonantesVista.Opcion;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Las importaciones de donantes del admin (R11-20), de a una página. {@code enCurso} cuenta las que
 * siguen importándose (un número de Donaciones, no el largo de una lista); {@code al}, cuándo se
 * leyeron las cifras ("12 de noviembre de 2026, 18:34"); {@code importaciones}, la página que se
 * muestra, ya buscada, filtrada y ordenada, con su total; {@code consulta}, lo que quedó aplicado;
 * {@code estados}, {@code subidas} y {@code ordenes}, las opciones de cada filtro (la primera es
 * "todas", valor '').
 */
public record AdminImportacionesVista(
    long enCurso,
    String al,
    Tramo<Fila> importaciones,
    ConsultaImportaciones consulta,
    List<Opcion> estados,
    List<Opcion> subidas,
    List<Opcion> ordenes) {

  /** Las opciones del filtro Estado (ficha r11-p1-importaciones, sección 4.1). */
  public static final List<Opcion> ESTADOS =
      List.of(
          new Opcion("", "Todos"),
          new Opcion("en-curso", "En curso"),
          new Opcion("terminadas", "Terminadas"),
          new Opcion("con-errores", "Terminadas con filas con error"),
          new Opcion("cortadas", "Se cortaron o no se importaron"));

  /** Las opciones de Ordenar. */
  public static final List<Opcion> ORDENES =
      List.of(
          new Opcion("", "Más recientes primero"), new Opcion("antiguas", "Más antiguas primero"));

  private static final DateTimeFormatter DIA_Y_MES =
      DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("es-AR"));

  public AdminImportacionesVista {
    estados = List.copyOf(estados);
    subidas = List.copyOf(subidas);
    ordenes = List.copyOf(ordenes);
  }

  /** La vista con las opciones de siempre para {@code consulta}. */
  public static AdminImportacionesVista de(
      long enCurso, String al, Tramo<Fila> importaciones, ConsultaImportaciones consulta) {
    return new AdminImportacionesVista(
        enCurso, al, importaciones, consulta, ESTADOS, subidas(consulta), ORDENES);
  }

  /**
   * Las opciones de Subida; con fechas escritas en la dirección, una más al final que las dice
   * ("Del 1 al 15 de octubre"), para que la pastilla muestre lo que se aplicó.
   */
  public static List<Opcion> subidas(ConsultaImportaciones consulta) {
    List<Opcion> subidas =
        new ArrayList<>(
            List.of(
                new Opcion("", "Cualquier fecha"),
                new Opcion("hoy", "Hoy"),
                new Opcion("7-dias", "Últimos 7 días"),
                new Opcion("30-dias", "Últimos 30 días"),
                new Opcion("este-mes", "Este mes"),
                new Opcion("mes-anterior", "El mes anterior")));
    if (consulta.conRango()) {
      subidas.add(new Opcion("rango", rango(consulta.desde(), consulta.hasta())));
    }
    return subidas;
  }

  /** "Del 1 al 15 de octubre", "Desde el 1 de octubre" o "Hasta el 15 de octubre". */
  static String rango(LocalDate desde, LocalDate hasta) {
    if (desde == null) {
      return "Hasta el " + DIA_Y_MES.format(hasta);
    }
    if (hasta == null) {
      return "Desde el " + DIA_Y_MES.format(desde);
    }
    if (desde.equals(hasta)) {
      return "El " + DIA_Y_MES.format(desde);
    }
    String inicio =
        desde.getMonth() == hasta.getMonth() && desde.getYear() == hasta.getYear()
            ? String.valueOf(desde.getDayOfMonth())
            : DIA_Y_MES.format(desde);
    return "Del " + inicio + " al " + DIA_Y_MES.format(hasta);
  }

  /**
   * Un renglón. {@code archivo} y {@code subido} ("15 oct 2026, 18:02") son null si Donaciones no
   * los guardó (importaciones viejas); {@code subidoIso}, la misma fecha para {@code time}. {@code
   * etapa} es {@code esperando}, {@code importando}, {@code terminada}, {@code con-errores}, {@code
   * cortada} o {@code error}; {@code estado}, cómo se dice; {@code filas}, lo leído ("8.400 de
   * 20.000"); {@code detalle}, el desglose o el porqué (null si no hay). {@code accion} ("Ver el
   * avance" o "Ver el resultado") y {@code enlace} son null si no hay a dónde ir.
   */
  public record Fila(
      String archivo,
      String subido,
      String subidoIso,
      String etapa,
      String estado,
      String filas,
      String detalle,
      String accion,
      String enlace) {}
}
