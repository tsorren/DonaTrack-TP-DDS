package grupo5.cliente.vistas;

import java.util.List;

/**
 * Donantes del admin (RF-15 y RF-20). {@code activos} cuenta los activos y {@code al} dice cuándo
 * se leyó ("15 de octubre de 2026, 18:30"). Los tres órdenes son las claves de {@code todos}: los
 * más nuevos primero, los más antiguos y por nombre. {@code lista} nombra de dónde salen ("la lista
 * de ejemplo", en la maqueta).
 */
public record AdminDonantesVista(
    long activos,
    String al,
    List<Donante> todos,
    List<String> ordenNuevos,
    List<String> ordenAntiguos,
    List<String> ordenNombre,
    String lista) {

  /**
   * Un renglón. {@code tipo} es {@code persona}, {@code empresa} u {@code organizacion}; {@code
   * estado}, {@code activo}, {@code pendiente} o {@code baja}; {@code dona}, la categoría del
   * filtro. {@code alta}, {@code dona} y {@code donaTexto} son null si no se saben.
   */
  public record Donante(
      String clave,
      String nombre,
      String tipo,
      String tipoTexto,
      String documento,
      String alta,
      String estado,
      String dona,
      String donaTexto) {}

  /**
   * Una importación de un CSV y su avance. {@code accion} es la pantalla que le toca: {@code
   * importando}, {@code importado} o {@code importar-error}. Los números ya vienen escritos
   * ("20.000"); {@code terminada} es la fecha de fin, o null si no se sabe; {@code resultado}, la
   * frase con lo que pasó; {@code filas}, las primeras filas con error; {@code avance}, la
   * dirección de "Ver el avance".
   */
  public record Importacion(
      String accion,
      String archivo,
      String total,
      String procesadas,
      String terminada,
      String resultado,
      String conError,
      List<FilaConError> filas,
      String avance) {}

  /** Una fila que no se importó: su número ("Fila 15") y qué pasó. */
  public record FilaConError(String fila, String que) {}
}
