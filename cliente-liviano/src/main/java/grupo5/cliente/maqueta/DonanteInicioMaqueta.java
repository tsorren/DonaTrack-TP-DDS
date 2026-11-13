package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.DonanteInicioPuerto;
import grupo5.cliente.vistas.DonanteAnioVista;
import grupo5.cliente.vistas.DonanteDonacionVista;
import grupo5.cliente.vistas.DonanteInicioVista;
import grupo5.cliente.vistas.DonanteInicioVista.Aviso;
import grupo5.cliente.vistas.DonanteTableroVista;
import grupo5.cliente.vistas.EstadoEntrega;
import grupo5.cliente.vistas.Tramo;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * El inicio de la empresa de ejemplo: las siete donaciones de viaje-datos, las tres noticias más
 * nuevas (las de datos-donante) y las fechas de la maqueta ("hoy" es el 15 de octubre de 2026). El
 * año de la persona sale de las mismas siete, todas de octubre: el mes muestra 3 y el total. El
 * tablero corta las siete por columna con {@link Tramo#de}; con {@code ?escenario=muchas}, suma
 * donaciones de ejemplo solo al tablero (1, 5 y 48, los totales del boceto A) para ver los
 * paginadores, sin cambiar las cifras ni el hito del inicio.
 */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.donante",
    havingValue = "maqueta",
    matchIfMissing = true)
class DonanteInicioMaqueta implements DonanteInicioPuerto {

  private static final DonanteInicioVista INICIO =
      new DonanteInicioVista(
          DonanteDonacionesDeEjemplo.DONACIONES,
          List.of(
              new Aviso(donacion("lentejas"), 0),
              new Aviso(donacion("arroz"), 0),
              new Aviso(donacion("leche"), 0)),
          4,
          2,
          "octubre de 2026",
          "15 de octubre",
          "15 de octubre, 16:25",
          "1 de octubre",
          "2026-10-15",
          "2026-10-14");

  private static final List<String> MESES =
      List.of(
          "enero",
          "febrero",
          "marzo",
          "abril",
          "mayo",
          "junio",
          "julio",
          "agosto",
          "septiembre",
          "octubre");

  private static final DonanteAnioVista ANIO =
      new DonanteAnioVista(
          2026,
          MESES.size(),
          IntStream.rangeClosed(1, MESES.size()).mapToObj(DonanteInicioMaqueta::mes).toList(),
          INICIO.donaciones().size(),
          contar(EstadoEntrega.ENTREGADA),
          contar(EstadoEntrega.EN_CAMINO),
          contar(EstadoEntrega.PENDIENTE),
          true,
          INICIO.avisos(),
          INICIO.sinLeer(),
          INICIO.hoy(),
          INICIO.actualizado(),
          INICIO.hoyIso(),
          INICIO.ayerIso());

  private static final Predicate<DonanteDonacionVista> TENER =
      d -> d.estado() == EstadoEntrega.CON_PROBLEMA;
  private static final Predicate<DonanteDonacionVista> CAMINO =
      d ->
          d.estado() == EstadoEntrega.EN_CAMINO
              || (d.estado() == EstadoEntrega.PENDIENTE && d.destino() != null);
  private static final Predicate<DonanteDonacionVista> LLEGO =
      d -> d.estado() == EstadoEntrega.ENTREGADA;

  /** El escenario de la maqueta que llena el tablero. */
  private static final String MUCHAS = "muchas";

  /** Nombres de ejemplo para las copias de {@code ?escenario=muchas}. */
  private static final List<String> OTRAS =
      List.of(
          "Aceite de girasol",
          "Frazadas",
          "Polenta",
          "Cuadernos",
          "Yerba",
          "Camperas",
          "Azúcar",
          "Lápices");

  private static final List<DonanteDonacionVista> MUCHAS_TENER = estiradas(TENER, 1);
  private static final List<DonanteDonacionVista> MUCHAS_CAMINO = estiradas(CAMINO, 5);
  private static final List<DonanteDonacionVista> MUCHAS_LLEGO = estiradas(LLEGO, 48);

  private static List<DonanteDonacionVista> de(Predicate<DonanteDonacionVista> columna) {
    return INICIO.donaciones().stream().filter(columna).toList();
  }

  /**
   * Las de la columna estiradas hasta {@code total}: las de ejemplo primero y después copias con
   * otros nombres (cada una lleva al detalle de la que copia).
   */
  private static List<DonanteDonacionVista> estiradas(
      Predicate<DonanteDonacionVista> columna, int total) {
    List<DonanteDonacionVista> propias = de(columna);
    Stream<DonanteDonacionVista> copias =
        IntStream.range(0, total - propias.size())
            .mapToObj(i -> copia(propias.get(i % propias.size()), OTRAS.get(i % OTRAS.size())));
    return Stream.concat(propias.stream(), copias).toList();
  }

  private static DonanteDonacionVista copia(DonanteDonacionVista d, String titulo) {
    return new DonanteDonacionVista(
        d.clave(),
        d.numero(),
        titulo,
        d.cantidad(),
        d.categoria(),
        d.categoriaTexto(),
        d.destino(),
        d.donada(),
        d.llego(),
        d.estado(),
        d.nota(),
        d.foto(),
        d.fotoAlt(),
        d.seguimiento(),
        d.pasos(),
        d.cartas());
  }

  private static DonanteAnioVista.Mes mes(int numero) {
    String nombre = MESES.get(numero - 1);
    List<DonanteDonacionVista> delMes =
        INICIO.donaciones().stream().filter(d -> d.donada().endsWith(nombre + " de 2026")).toList();
    return new DonanteAnioVista.Mes(
        numero,
        nombre,
        "2026-%02d".formatted(numero),
        delMes.size(),
        delMes.stream().limit(DonanteAnioVista.POR_MES).toList());
  }

  private static long contar(EstadoEntrega estado) {
    return INICIO.donaciones().stream().filter(d -> d.estado() == estado).count();
  }

  private static DonanteDonacionVista donacion(String clave) {
    return DonanteDonacionesDeEjemplo.DONACIONES.stream()
        .filter(d -> d.clave().equals(clave))
        .findFirst()
        .orElseThrow();
  }

  @Override
  public DonanteInicioVista inicio(UUID donante) {
    return INICIO;
  }

  @Override
  public DonanteTableroVista tablero(UUID donante, int tener, int camino, int llego) {
    boolean muchas = MUCHAS.equals(escenario());
    int porColumna = DonanteTableroVista.POR_COLUMNA;
    return new DonanteTableroVista(
        Tramo.de(muchas ? MUCHAS_TENER : de(TENER), tener, porColumna),
        Tramo.de(muchas ? MUCHAS_CAMINO : de(CAMINO), camino, porColumna),
        Tramo.de(muchas ? MUCHAS_LLEGO : de(LLEGO), llego, porColumna),
        INICIO.donaciones().stream()
            .filter(d -> d.estado() == EstadoEntrega.PENDIENTE && d.destino() == null)
            .count());
  }

  private static String escenario() {
    return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes pedido
        ? pedido.getRequest().getParameter("escenario")
        : null;
  }

  @Override
  public DonanteAnioVista anio(UUID donante) {
    return ANIO;
  }
}
