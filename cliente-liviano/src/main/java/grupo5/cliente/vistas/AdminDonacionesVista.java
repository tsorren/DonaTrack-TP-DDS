package grupo5.cliente.vistas;

import java.util.ArrayList;
import java.util.List;

/**
 * Donaciones del admin (RF-15 y RF-16), de a una página (R11-01). {@code donaciones} es la página
 * que se muestra, ya filtrada y ordenada por la fuente, con su total; {@code consulta}, el filtro y
 * el orden que quedaron aplicados (un orden que la fuente no ofrece se toma como el de partida);
 * {@code criterios}, los órdenes que se pueden elegir (el primero es el de partida, valor '');
 * {@code conVence}, si la fuente filtra "vencen en los próximos 7 días" (si no, el filtro no se
 * ofrece y {@code porVencer} dice cuántas vencen); {@code resumen}, las cifras de la cabecera con
 * su fecha.
 *
 * <p>Desde la bandeja (R11-34): {@code categorias}, las del catálogo para la pastilla Categoría
 * (sin la de "todas"); {@code donante}, el nombre del donante del filtro que llega desde su ficha
 * (null si no hay); {@code elegida}, la donación cuya ficha se muestra al lado de la lista (null si
 * ninguna); {@code partida}, si la elegida es la primera de la página porque no se pidió ninguna
 * (en el celular su ficha no se abre sola).
 */
public record AdminDonacionesVista(
    Tramo<Donacion> donaciones,
    ConsultaDonaciones consulta,
    List<Criterio> criterios,
    boolean conVence,
    String porVencer,
    String resumen,
    List<Opcion> categorias,
    String donante,
    Donacion elegida,
    boolean partida) {

  /** Las opciones de la pastilla "Registrada": la primera es "Cualquier fecha". */
  public static final List<Opcion> PERIODOS =
      PeriodoFijo.OPCIONES.stream().map(p -> new Opcion(p.clave(), p.texto())).toList();

  public AdminDonacionesVista {
    criterios = List.copyOf(criterios);
    categorias = categorias == null ? List.of() : List.copyOf(categorias);
  }

  /** Sin lo de la bandeja: la vista de antes de R11-34. */
  public AdminDonacionesVista(
      Tramo<Donacion> donaciones,
      ConsultaDonaciones consulta,
      List<Criterio> criterios,
      boolean conVence,
      String porVencer,
      String resumen) {
    this(
        donaciones,
        consulta,
        criterios,
        conVence,
        porVencer,
        resumen,
        List.of(),
        null,
        null,
        false);
  }

  /** La misma vista con la ficha de {@code otra} (null: sin ficha). */
  public AdminDonacionesVista conElegida(Donacion otra, boolean dePartida) {
    return new AdminDonacionesVista(
        donaciones,
        consulta,
        criterios,
        conVence,
        porVencer,
        resumen,
        categorias,
        donante,
        otra,
        dePartida);
  }

  /**
   * Si la elegida no está en la página que se muestra (se pidió por su clave desde otra página o
   * desde otra pantalla): su ficha va al final de la lista.
   */
  public boolean elegidaFuera() {
    return elegida != null
        && donaciones != null
        && donaciones.filas().stream().noneMatch(d -> d.clave().equals(elegida.clave()));
  }

  /** Las opciones de la pastilla Estado: la primera es "Todas". */
  public List<Opcion> estados() {
    return List.of(
        new Opcion("", "Todas"),
        new Opcion("deposito", "En el depósito"),
        new Opcion("asignada", "Asignada"),
        new Opcion("camino", "En camino"),
        new Opcion("entregada", "Entregada"),
        new Opcion("vencida", "Vencida"));
  }

  /** Las opciones de la pastilla Categoría: "Todas las categorías" y las del catálogo. */
  public List<Opcion> categoriasDelFiltro() {
    List<Opcion> opciones = new ArrayList<>();
    opciones.add(new Opcion("", "Todas las categorías"));
    opciones.addAll(categorias);
    return List.copyOf(opciones);
  }

  /** Las opciones de la pastilla Registrada. */
  public List<Opcion> periodos() {
    return PERIODOS;
  }

  /** Las opciones de la pastilla Vence. */
  public List<Opcion> vencimientos() {
    return List.of(new Opcion("", "Cuando sea"), new Opcion("pronto", "En los próximos 7 días"));
  }

  /** Las opciones de la pastilla Ordenar: los criterios de la fuente. */
  public List<Opcion> ordenes() {
    return criterios.stream().map(c -> new Opcion(c.valor(), c.texto())).toList();
  }

  /** Una opción de una pastilla de filtro: su valor en la dirección y su texto. */
  public record Opcion(String valor, String texto) {}

  /** Un orden de la lista: su valor en la dirección, su texto y el título de la tabla. */
  public record Criterio(String valor, String texto, String caption) {}

  /** {@code valor} si es uno de {@code criterios}; si no, '' (el de partida). */
  public static String elegido(String valor, List<Criterio> criterios) {
    return valor != null && criterios.stream().anyMatch(c -> c.valor().equals(valor)) ? valor : "";
  }

  /**
   * Una donación. {@code estado} es {@code deposito}, {@code asignada}, {@code camino}, {@code
   * entregada} o {@code vencida}; {@code vence}, la fecha más próxima de sus bienes, o null; {@code
   * prontaAVencer}, si está en el depósito y vence en los próximos 7 días. {@code bienes}, los de
   * una donación de varios bienes (null si tiene uno). {@code historial}, cada cambio ya escrito
   * para la pantalla. {@code nueva}: la recién registrada de la maqueta.
   */
  public record Donacion(
      String clave,
      String que,
      String cantidad,
      String corto,
      String donante,
      String registrada,
      String estado,
      String vence,
      boolean prontaAVencer,
      String bien,
      String deposito,
      String entidad,
      boolean nueva,
      List<Bien> bienes,
      List<String> historial) {

    public Donacion {
      bienes = bienes == null ? null : List.copyOf(bienes);
      historial = List.copyOf(historial);
    }
  }

  /** Un bien de la donación: qué es, cuánto, en qué estado y cuándo vence (o null). */
  public record Bien(String que, String cantidad, String bien, String vence) {}
}
