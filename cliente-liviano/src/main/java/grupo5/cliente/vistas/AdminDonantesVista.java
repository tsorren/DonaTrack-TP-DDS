package grupo5.cliente.vistas;

import java.util.List;

/**
 * Donantes del admin (RF-15 y RF-20), de a una página (R11-01). {@code activos} cuenta los activos
 * (un número de Donaciones, no el largo de una lista) y {@code al} dice cuándo se leyó ("15 de
 * octubre de 2026, 18:30"). {@code donantes} es la página que se muestra, ya buscada, filtrada y
 * ordenada, con su total; {@code consulta}, la búsqueda y los filtros que quedaron aplicados (los
 * valores que no son de {@code opciones} se descartan); {@code opciones}, los filtros, los órdenes
 * y las columnas que se pueden ofrecer con lo que da la fuente. {@code importacion}, la última
 * importación que subió el admin en esta sesión, si sigue en curso o terminó y no abrió su
 * resultado (la franja de aviso); null si no hay.
 */
public record AdminDonantesVista(
    long activos,
    String al,
    Tramo<Donante> donantes,
    ConsultaDonantes consulta,
    Opciones opciones,
    Importacion importacion) {

  /** La misma vista con la franja de la importación de la sesión. */
  public AdminDonantesVista conImportacion(Importacion enCurso) {
    return new AdminDonantesVista(activos, al, donantes, consulta, opciones, enCurso);
  }

  /**
   * Lo que se puede filtrar y ordenar. Cada lista empieza con su opción "todos" (valor ''). {@code
   * categorias} vacía: no hay filtro "Qué dona". {@code conDona} y {@code conAlta}: si la fuente da
   * qué dona cada donante y su fecha de alta (sin eso, la columna no se muestra).
   */
  public record Opciones(
      List<Opcion> tipos,
      List<Opcion> categorias,
      List<Opcion> estados,
      List<Opcion> ordenes,
      boolean conDona,
      boolean conAlta) {

    public Opciones {
      tipos = List.copyOf(tipos);
      categorias = List.copyOf(categorias);
      estados = List.copyOf(estados);
      ordenes = List.copyOf(ordenes);
    }

    /** {@code valor} si es una de {@code opciones}; si no, '' (todos). */
    public static String elegido(String valor, List<Opcion> opciones) {
      return valor != null && opciones.stream().anyMatch(o -> o.valor().equals(valor)) ? valor : "";
    }
  }

  /** Una opción de un filtro: el valor de la dirección y su texto. */
  public record Opcion(String valor, String texto) {}

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
   * Una importación de un CSV y su avance (R11-02). {@code accion} es la pantalla que le toca:
   * {@code importando}, {@code importado} o {@code importar-error}; {@code etapa}, en qué está:
   * {@code esperando} (todavía no empezó), {@code leyendo} (sin el total de filas), {@code
   * importando}, {@code cortada} (Donaciones la dejó en error después de leer filas), {@code
   * terminada} o {@code error} (no se importó ninguna fila). Los números ya vienen escritos
   * ("20.000"); {@code leidas} y {@code totalFilas}, sin escribir, son el valor y el máximo de la
   * barra (total null: la barra no tiene medida). {@code resultado} es la frase con lo que pasó;
   * {@code ultimos}, los últimos donantes que leyó el archivo; {@code filas}, la página de filas
   * con error que se muestra; {@code avance}, la dirección de la página de avance; {@code hora},
   * cuándo se leyó ("18:34:12"). {@code subido} ("15 de octubre de 2026, 18:02") y {@code
   * subidoIso} (R11-20): cuándo se subió el archivo, según Donaciones; null si no lo guardó. {@code
   * id}, el de la importación en Donaciones (null en la maqueta): arma la descarga de las filas con
   * error.
   */
  public record Importacion(
      String accion,
      String etapa,
      String archivo,
      String total,
      String procesadas,
      String creadas,
      String actualizadas,
      String conError,
      int leidas,
      Integer totalFilas,
      String resultado,
      List<Ultimo> ultimos,
      Tramo<FilaConError> filas,
      String avance,
      String hora,
      String subido,
      String subidoIso,
      String id) {

    public Importacion {
      ultimos = ultimos == null ? List.of() : List.copyOf(ultimos);
    }

    /** Sin la fecha de subida ni el id: la forma anterior a R11-20. */
    public Importacion(
        String accion,
        String etapa,
        String archivo,
        String total,
        String procesadas,
        String creadas,
        String actualizadas,
        String conError,
        int leidas,
        Integer totalFilas,
        String resultado,
        List<Ultimo> ultimos,
        Tramo<FilaConError> filas,
        String avance,
        String hora) {
      this(
          accion,
          etapa,
          archivo,
          total,
          procesadas,
          creadas,
          actualizadas,
          conError,
          leidas,
          totalFilas,
          resultado,
          ultimos,
          filas,
          avance,
          hora,
          null,
          null,
          null);
    }

    /** Si todavía se está importando: la página de avance se sigue actualizando sola. */
    public boolean enCurso() {
      return "importando".equals(accion);
    }
  }

  /** Un donante que leyó el archivo: su nombre y "Nuevo" o "Actualizado". */
  public record Ultimo(String nombre, String resultado) {}

  /** Una fila que no se importó: su número ("Fila 15") y qué pasó. */
  public record FilaConError(String fila, String que) {}
}
