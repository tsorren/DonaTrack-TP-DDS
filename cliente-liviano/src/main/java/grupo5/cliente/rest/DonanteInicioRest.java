package grupo5.cliente.rest;

import static grupo5.cliente.rest.Comun.ARGENTINA;
import static grupo5.cliente.rest.Comun.DIA;
import static grupo5.cliente.rest.Comun.DIA_Y_HORA;
import static grupo5.cliente.rest.Comun.ES_AR;
import static grupo5.cliente.vistas.DonanteAnioVista.POR_MES;

import grupo5.cliente.puertos.DonanteInicioPuerto;
import grupo5.cliente.vistas.DonanteAnioVista;
import grupo5.cliente.vistas.DonanteDonacionVista;
import grupo5.cliente.vistas.DonanteInicioVista;
import grupo5.cliente.vistas.DonanteInicioVista.Aviso;
import grupo5.cliente.vistas.DonanteTableroVista;
import grupo5.cliente.vistas.EstadoEntrega;
import grupo5.cliente.vistas.Tramo;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;

/**
 * El inicio del donante: sus donaciones como las arma Seguimiento (Donaciones y, de las que van en
 * curso, Logística), las tres noticias más nuevas de su historial y las sin leer de Notificaciones
 * (0 si no responde). Las entregas del mes son las confirmadas en el mes de hoy. El año de la
 * persona ({@link #anio}) no pide la lista entera: de cada mes, una página de 3 con su total, y las
 * cifras del año como totales ({@code X-Total-Count}) de páginas de una. El tablero de la empresa
 * ({@link #tablero}) tampoco: una página de 3 por columna con su total, y la cifra de las que
 * esperan destino.
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.donante", havingValue = "rest")
class DonanteInicioRest implements DonanteInicioPuerto {

  private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", ES_AR);
  private static final DateTimeFormatter NOMBRE_DEL_MES =
      DateTimeFormatter.ofPattern("MMMM", ES_AR);
  private static final int NOTICIAS = 3;
  private static final List<String> ENTREGADAS = List.of("ENTREGADA");
  private static final List<String> EN_CAMINO = List.of("EN_TRASLADO", "EN_PUERTA");
  private static final List<String> EN_DEPOSITO =
      List.of("EN_DEPOSITO", "ASIGNACION_REALIZADA", "LISTA_PARA_ENTREGAR");

  // Los grupos de estados de cada columna del tablero (ficha r11-p2-tablero, §4.3), en un solo
  // lugar: los mismos que separaba la plantilla.
  private static final List<String> COLUMNA_TENER = List.of("ENTREGA_FALLIDA", "VENCIDA");
  private static final List<String> COLUMNA_CAMINO =
      List.of("EN_TRASLADO", "EN_PUERTA", "ASIGNACION_REALIZADA", "LISTA_PARA_ENTREGAR");
  private static final List<String> COLUMNA_LLEGO = ENTREGADAS;
  private static final List<String> ESPERAN_DESTINO = List.of("EN_DEPOSITO");

  private final DonacionesApi api;
  private final LogisticaApi logistica;
  private final NotificacionesApi notificaciones;
  private final Clock reloj;

  DonanteInicioRest(
      DonacionesApi api, LogisticaApi logistica, NotificacionesApi notificaciones, Clock reloj) {
    this.api = api;
    this.logistica = logistica;
    this.notificaciones = notificaciones;
    this.reloj = reloj;
  }

  @Override
  public DonanteInicioVista inicio(UUID donante) {
    LocalDateTime ahora = LocalDateTime.now(reloj.withZone(ARGENTINA));
    LocalDate hoy = ahora.toLocalDate();
    List<DonanteDonacionVista> donaciones =
        DonanteDonaciones.de(
            api, donante, DonanteSeguimientoRest.seguimientos(logistica, donante), hoy);
    String mes = hoy.format(MES);
    // "5 de octubre de 2026" termina con el mes y el año de hoy si llegó este mes.
    int delMes =
        (int)
            donaciones.stream()
                .filter(d -> d.estado() == EstadoEntrega.ENTREGADA && d.llego() != null)
                .filter(d -> d.llego().endsWith(mes))
                .count();
    List<Aviso> avisos =
        donaciones.stream()
            .flatMap(d -> IntStream.range(0, d.cartas().size()).mapToObj(i -> new Aviso(d, i)))
            .sorted(Comparator.comparing((Aviso a) -> a.d().cartas().get(a.i()).fecha()).reversed())
            .limit(NOTICIAS)
            .toList();
    // La primera donación: "1 de octubre", con el año solo si no es el de hoy.
    String desde =
        donaciones.isEmpty()
            ? hoy.format(DIA)
            : donaciones.getLast().donada().replace(" de " + hoy.getYear(), "");
    return new DonanteInicioVista(
        donaciones,
        avisos,
        sinLeer(donante),
        delMes,
        mes,
        hoy.format(DIA),
        ahora.format(DIA_Y_HORA),
        desde,
        hoy.toString(),
        hoy.minusDays(1).toString());
  }

  @Override
  public DonanteTableroVista tablero(UUID donante, int tener, int camino, int llego) {
    LocalDate hoy = LocalDate.now(reloj.withZone(ARGENTINA));
    Pagina paginaTener = columna(donante, COLUMNA_TENER, tener);
    Long esperan = null;
    if (paginaTener != null) {
      try {
        esperan = contar(donante, null, null, ESPERAN_DESTINO);
      } catch (RestClientException sinCifra) {
        // Las que esperan destino van en la misma tarjeta: sin la cifra, la columna dice que falló.
        paginaTener = null;
      }
    }
    Pagina paginaCamino = columna(donante, COLUMNA_CAMINO, camino);
    Pagina paginaLlego = columna(donante, COLUMNA_LLEGO, llego);
    List<DonacionesApi.Donacion> mostradas =
        Stream.of(paginaTener, paginaCamino, paginaLlego)
            .filter(Objects::nonNull)
            .flatMap(p -> p.filas().stream())
            .toList();
    Map<UUID, String> entidades = DonacionesRest.nombresDeEntidades(api, mostradas);
    // "Sale del depósito el…" lo dice Logística: solo si la columna en curso trae alguna.
    Map<UUID, LogisticaApi.Seguimiento> seguimientos =
        paginaCamino == null || paginaCamino.filas().isEmpty()
            ? Map.of()
            : DonanteSeguimientoRest.seguimientos(logistica, donante);
    Function<Pagina, Tramo<DonanteDonacionVista>> tramo =
        p ->
            p == null
                ? null
                : new Tramo<>(
                    DonanteDonaciones.vistas(p.filas(), entidades, seguimientos, hoy),
                    p.total(),
                    p.pagina(),
                    Tramo.paginas(p.total(), DonanteTableroVista.POR_COLUMNA));
    return new DonanteTableroVista(
        tramo.apply(paginaTener),
        tramo.apply(paginaCamino),
        tramo.apply(paginaLlego),
        esperan == null ? 0 : esperan);
  }

  /** Lo que llegó de Donaciones para una columna: sus filas, el total y la página (desde 1). */
  private record Pagina(List<DonacionesApi.Donacion> filas, long total, int pagina) {}

  /**
   * La página {@code pedida} (desde 1) de los {@code estados}; si se pasa, la última que hay. Null
   * si Donaciones no responde.
   */
  private Pagina columna(UUID donante, List<String> estados, int pedida) {
    try {
      Pagina pagina = leer(donante, estados, pedida);
      int ultima = Tramo.paginas(pagina.total(), DonanteTableroVista.POR_COLUMNA);
      return pagina.pagina() > ultima ? leer(donante, estados, ultima) : pagina;
    } catch (RestClientException sinColumna) {
      return null;
    }
  }

  private Pagina leer(UUID donante, List<String> estados, int pagina) {
    MultiValueMap<String, String> filtros =
        filtros(donante, null, null, estados, DonanteTableroVista.POR_COLUMNA);
    filtros.set("page", String.valueOf(pagina - 1));
    ResponseEntity<List<DonacionesApi.Donacion>> respuesta = api.pagina(filtros);
    List<DonacionesApi.Donacion> cuerpo =
        respuesta.getBody() == null ? List.of() : respuesta.getBody();
    return new Pagina(cuerpo, total(respuesta, cuerpo.size()), pagina);
  }

  @Override
  public DonanteAnioVista anio(UUID donante) {
    LocalDateTime ahora = LocalDateTime.now(reloj.withZone(ARGENTINA));
    LocalDate hoy = ahora.toLocalDate();
    List<YearMonth> meses =
        IntStream.rangeClosed(1, hoy.getMonthValue())
            .mapToObj(m -> YearMonth.of(hoy.getYear(), m))
            .toList();
    List<List<DonacionesApi.Donacion>> primeras = new ArrayList<>();
    List<Long> totales = new ArrayList<>();
    for (YearMonth mes : meses) {
      ResponseEntity<List<DonacionesApi.Donacion>> pagina =
          api.pagina(filtros(donante, mes.atDay(1), mes.atEndOfMonth(), List.of(), POR_MES));
      List<DonacionesApi.Donacion> cuerpo = pagina.getBody() == null ? List.of() : pagina.getBody();
      primeras.add(cuerpo);
      totales.add(total(pagina, cuerpo.size()));
    }
    List<DonacionesApi.Donacion> mostradas = primeras.stream().flatMap(List::stream).toList();
    List<DonanteDonacionVista> vistas =
        DonanteDonaciones.vistas(
            mostradas, DonacionesRest.nombresDeEntidades(api, mostradas), Map.of(), hoy);
    List<DonanteAnioVista.Mes> delAnio = new ArrayList<>();
    int desde = 0;
    for (int i = 0; i < meses.size(); i++) {
      int hasta = desde + primeras.get(i).size();
      delAnio.add(
          new DonanteAnioVista.Mes(
              meses.get(i).getMonthValue(),
              meses.get(i).format(NOMBRE_DEL_MES),
              meses.get(i).toString(),
              totales.get(i),
              vistas.subList(desde, hasta)));
      desde = hasta;
    }
    long total = totales.stream().mapToLong(Long::longValue).sum();
    LocalDate enero = hoy.withDayOfYear(1);
    LocalDate diciembre = enero.withMonth(12).withDayOfMonth(31);
    List<Aviso> avisos =
        vistas.stream()
            .flatMap(d -> IntStream.range(0, d.cartas().size()).mapToObj(i -> new Aviso(d, i)))
            .sorted(Comparator.comparing((Aviso a) -> a.d().cartas().get(a.i()).fecha()).reversed())
            .limit(NOTICIAS)
            .toList();
    return new DonanteAnioVista(
        hoy.getYear(),
        hoy.getMonthValue(),
        delAnio,
        total,
        total == 0 ? 0 : contar(donante, enero, diciembre, ENTREGADAS),
        total == 0 ? 0 : contar(donante, enero, diciembre, EN_CAMINO),
        total == 0 ? 0 : contar(donante, enero, diciembre, EN_DEPOSITO),
        total > 0 || contar(donante, null, null, List.of()) > 0,
        avisos,
        sinLeer(donante),
        hoy.format(DIA),
        ahora.format(DIA_Y_HORA),
        hoy.toString(),
        hoy.minusDays(1).toString());
  }

  /** Cuántas donaciones cumplen los filtros: el total de una página de una. */
  private long contar(UUID donante, LocalDate desde, LocalDate hasta, List<String> estados) {
    ResponseEntity<List<DonacionesApi.Donacion>> pagina =
        api.pagina(filtros(donante, desde, hasta, estados, 1));
    return total(pagina, pagina.getBody() == null ? 0 : pagina.getBody().size());
  }

  private static MultiValueMap<String, String> filtros(
      UUID donante, LocalDate desde, LocalDate hasta, List<String> estados, int tamanio) {
    MultiValueMap<String, String> filtros = new LinkedMultiValueMap<>();
    filtros.add("donanteId", donante.toString());
    if (desde != null) {
      filtros.add("desde", desde.toString());
      filtros.add("hasta", hasta.toString());
    }
    estados.forEach(e -> filtros.add("estado", e));
    filtros.add("page", "0");
    filtros.add("size", String.valueOf(tamanio));
    return filtros;
  }

  /** El total de {@code X-Total-Count}; sin la cabecera, lo que vino en la página. */
  private static long total(ResponseEntity<?> respuesta, int enLaPagina) {
    String total = respuesta.getHeaders().getFirst("X-Total-Count");
    try {
      return total == null ? enLaPagina : Long.parseLong(total);
    } catch (NumberFormatException malEscrito) {
      return enLaPagina;
    }
  }

  private long sinLeer(UUID persona) {
    try {
      NotificacionesApi.Conteo conteo = notificaciones.sinLeer(persona);
      return conteo == null || conteo.noLeidas() == null ? 0 : conteo.noLeidas();
    } catch (RestClientException sinNotificaciones) {
      return 0;
    }
  }
}
