package grupo5.cliente.vistas;

import java.util.List;

/**
 * Necesidades de la entidad (RF-10). {@code al}, el día de la lectura ("15 de octubre de 2026");
 * {@code hoy} y {@code hoyTexto}, el mismo para la fecha mínima de Publicar ("2026-10-15", "15 de
 * octubre"); {@code todas}, las vigentes; {@code rubros}, los canteros por categoría; {@code
 * terminadas}, las cumplidas, terminadas y dadas de baja; {@code catalogo}, lo que se puede pedir;
 * {@code aConfirmar}, la clave de la primera entrega que espera en la puerta, y {@code
 * conProblema}, la de la primera necesidad con una entrega con problema (o null).
 */
public record EntidadNecesidadesVista(
    String al,
    String hoy,
    String hoyTexto,
    List<Necesidad> todas,
    List<Rubro> rubros,
    List<Terminada> terminadas,
    List<Grupo> catalogo,
    String aConfirmar,
    String conProblema) {

  /**
   * Una necesidad vigente. {@code clave} la nombra en la dirección y en los ids; {@code id} y
   * {@code version}, los de Donaciones para cambiarla o darla de baja (null en la maqueta). {@code
   * valor}, lo recibido (del período en curso, si se repite), o null si no se sabe; {@code max}, lo
   * que pide. {@code cambio} y {@code cambiada}, el cartel y la marca del último cambio, o null.
   * {@code puerta}, {@code camino} y {@code problema}, cuántas entregas hay de cada una.
   */
  public record Necesidad(
      String clave,
      String id,
      Long version,
      String nombre,
      String categoria,
      String rubro,
      String unidad,
      String plazo,
      String faltan,
      String repite,
      Integer valor,
      int max,
      String cambio,
      String cambiada,
      String motivo,
      int puerta,
      int camino,
      int problema,
      List<Entrega> entregas,
      List<Donacion> donaron,
      String desde,
      List<Periodo> periodos) {}

  /**
   * Una entrega para la necesidad: {@code tono}, el del sello ({@code puerta}, {@code en-camino},
   * {@code pendiente} o {@code con-problema}); {@code confirmar}, la clave para confirmarla, o
   * null.
   */
  public record Entrega(String cuanto, String tono, String texto, String confirmar) {}

  /**
   * Quién donó: {@code quien}, null si ocultó su nombre, con {@code categoriaDonante} (la del
   * ranking) o null y {@code tipo} ({@code empresa} o null).
   */
  public record Donacion(
      String quien, String categoriaDonante, String tipo, String cuanto, String cuando) {}

  /**
   * Un período: {@code tono} es {@code curso}, {@code todo}, {@code parte} o {@code nada}; {@code
   * fechas}, solo en el que está en curso; {@code max}, su objetivo, o null si es el de la
   * necesidad.
   */
  public record Periodo(
      String nombre,
      String fechas,
      String recibido,
      String tono,
      String estado,
      Integer valor,
      Integer max) {}

  /** Un cantero: {@code clave} para su id; {@code icono}, el del sprite, o null. */
  public record Rubro(String clave, String nombre, String icono) {}

  /** Una cumplida o terminada: {@code titulo} en negrita ("Útiles escolares · 30 unidades"). */
  public record Terminada(String titulo, String texto) {}

  /** Un grupo de lo que se puede pedir, con sus opciones. */
  public record Grupo(String nombre, List<Opcion> opciones) {}

  /**
   * Una opción de Qué hace falta: {@code id}, la subcategoría (null en la maqueta); {@code
   * elegida}, la que vuelve elegida con el error de datos.
   */
  public record Opcion(String id, String texto, boolean elegida) {}
}
