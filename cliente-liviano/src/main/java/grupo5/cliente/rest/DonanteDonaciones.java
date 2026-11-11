package grupo5.cliente.rest;

import static grupo5.cliente.rest.Comun.ARGENTINA;
import static grupo5.cliente.rest.Comun.DIA_Y_ANIO;
import static grupo5.cliente.rest.Comun.DIA_Y_HORA;
import static grupo5.cliente.rest.Comun.ENTREGADA;
import static grupo5.cliente.rest.Comun.FALLIDA;
import static grupo5.cliente.rest.Comun.HORA;
import static grupo5.cliente.rest.Comun.PUERTA;
import static grupo5.cliente.rest.Comun.TRASLADO;
import static grupo5.cliente.rest.Comun.cantidad;
import static grupo5.cliente.rest.Comun.categoria;
import static grupo5.cliente.rest.Comun.desde;
import static grupo5.cliente.rest.Comun.icono;
import static grupo5.cliente.rest.Comun.normalizado;

import grupo5.cliente.rest.DonacionesApi.Cambio;
import grupo5.cliente.rest.DonacionesApi.Categoria;
import grupo5.cliente.vistas.DonanteDonacionVista;
import grupo5.cliente.vistas.DonanteDonacionVista.Carta;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Las donaciones del donante como las muestran el inicio y Seguimiento, armadas con lo mismo que
 * Mis donaciones ({@link DonacionesRest}: etapas, nota y noticias del historial) más lo que dice
 * Logística de las que están en curso. Sin foto: la foto de la entidad la trae el detalle.
 */
final class DonanteDonaciones {

  private DonanteDonaciones() {}

  /** La más reciente primero; {@code seguimientos}, por id de donación (vacío sin Logística). */
  static List<DonanteDonacionVista> de(
      DonacionesApi api,
      UUID donante,
      Map<UUID, LogisticaApi.Seguimiento> seguimientos,
      LocalDate hoy) {
    List<DonacionesApi.Donacion> lista =
        api.donaciones(donante).stream()
            .sorted(Comparator.comparing(DonacionesApi.Donacion::fechaRegistro).reversed())
            .toList();
    Map<UUID, String> entidades = DonacionesRest.nombresDeEntidades(api, lista);
    return IntStream.range(0, lista.size())
        .mapToObj(
            i ->
                donacion(
                    lista.get(i),
                    (long) lista.size() - i,
                    entidades,
                    seguimientos.get(lista.get(i).id()),
                    hoy))
        .toList();
  }

  private static DonanteDonacionVista donacion(
      DonacionesApi.Donacion d,
      long numero,
      Map<UUID, String> entidades,
      LogisticaApi.Seguimiento seguimiento,
      LocalDate hoy) {
    Categoria categoria = categoria(d);
    String estado = normalizado(d.estadoActual());
    String destino = entidades.get(d.entidadBeneficiariaId());
    String entidad = destino == null ? "la entidad" : destino;
    LocalDateTime desde = desde(d, estado);
    return new DonanteDonacionVista(
        d.id().toString(),
        numero,
        d.descripcion(),
        cantidad(d.cantidad(), categoria),
        categoria == null ? null : icono(categoria.nombre()),
        categoria == null ? null : categoria.nombre(),
        destino,
        d.fechaRegistro().format(DIA_Y_ANIO),
        ENTREGADA.equals(estado) ? desde.format(DIA_Y_ANIO) : null,
        DonacionesRest.estadoEntrega(estado),
        DonacionesRest.nota(estado, desde),
        null,
        null,
        hojaDeRuta(d, estado, seguimiento, hoy),
        DonacionesRest.pasos(d, estado),
        cartas(d, entidad));
  }

  /**
   * Lo que dice Seguimiento de una entrega en curso: la posición del camión con su hora si va en
   * camino, o el motivo si no pudo llegar; null en las demás.
   */
  private static String hojaDeRuta(
      DonacionesApi.Donacion d, String estado, LogisticaApi.Seguimiento s, LocalDate hoy) {
    if (FALLIDA.equals(estado)) {
      String motivo = DonacionesRest.motivo(d, estado);
      return (motivo == null ? "" : mayuscula(conPunto(motivo)) + " ")
          + "Última noticia: "
          + desde(d, estado).format(DIA_Y_HORA)
          + ".";
    }
    if (!(TRASLADO.equals(estado) || PUERTA.equals(estado))
        || s == null
        || s.posicionCamion() == null) {
      return null;
    }
    Instant leida = s.posicionCamion().registradaEn();
    if (leida == null) {
      leida = s.ultimaActualizacion();
    }
    if (leida == null) {
      return null;
    }
    LocalDateTime momento = LocalDateTime.ofInstant(leida, ARGENTINA);
    return "Posición aproximada, actualizada "
        + (momento.toLocalDate().equals(hoy)
            ? "hoy a las " + momento.format(HORA)
            : "el " + momento.format(DIA_Y_HORA))
        + ".";
  }

  /** Las noticias del historial, de la más nueva a la primera, como las del detalle. */
  private static List<Carta> cartas(DonacionesApi.Donacion d, String entidad) {
    if (d.historial() == null) {
      return List.of();
    }
    return d.historial().stream()
        .sorted(Comparator.comparing(Cambio::timestamp).reversed())
        .map(
            c ->
                new Carta(
                    DonacionesRest.textoDeNoticia(c, entidad, false),
                    c.timestamp().format(DIA_Y_HORA),
                    c.timestamp().toString()))
        .filter(c -> c.texto() != null)
        .toList();
  }

  private static String mayuscula(String texto) {
    return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
  }

  private static String conPunto(String texto) {
    String limpio = texto.strip();
    return limpio.endsWith(".") ? limpio : limpio + ".";
  }
}
