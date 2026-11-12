package grupo5.cliente.rest;

import static grupo5.cliente.rest.Comun.ASIGNADA;
import static grupo5.cliente.rest.Comun.DIA;
import static grupo5.cliente.rest.Comun.DIA_Y_HORA;
import static grupo5.cliente.rest.Comun.ENTREGADA;
import static grupo5.cliente.rest.Comun.FALLIDA;
import static grupo5.cliente.rest.Comun.LISTA;
import static grupo5.cliente.rest.Comun.PUERTA;
import static grupo5.cliente.rest.Comun.TRASLADO;
import static grupo5.cliente.rest.Comun.VENCIDA;

import grupo5.cliente.vistas.AdminDonacionesVista;
import grupo5.cliente.vistas.AdminDonacionesVista.Bien;
import grupo5.cliente.vistas.AdminDonacionesVista.Criterio;
import grupo5.cliente.vistas.AdminDonacionesVista.Donacion;
import grupo5.cliente.vistas.ConsultaDonaciones;
import grupo5.cliente.vistas.Tramo;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Las donaciones del admin desde Donaciones, de a una página (R11-01): la página de {@code GET
 * /donaciones-independientes} con el estado y el orden resueltos allá, los nombres de quienes
 * donaron de una sola vez ({@code GET /api/personas?id=…}) y las cifras de la cabecera de {@code
 * GET /api/donaciones/resumen}. El detalle se pide por su id, con su donación original (quién donó
 * y el depósito) y su entidad. Nunca una lista entera. Lo usa {@link AdminOperacionRest}.
 */
final class AdminOperacionDonaciones {

  private static final String DEPOSITO = "deposito";
  private static final String EN_ASIGNADA = "asignada";
  private static final String EN_CAMINO = "camino";
  private static final String EN_ENTREGADA = "entregada";
  private static final String ENDEPOSITO = "ENDEPOSITO";
  private static final String EN_DEPOSITO = "EN_DEPOSITO";
  private static final int PRONTO = 7;

  /** De a 20 por página (ficha, decisiones de R11). */
  static final int POR_PAGINA = 20;

  private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMMM", Comun.ES_AR);
  private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm", Comun.ES_AR);

  /** Los estados de Donaciones que entran en cada estado de la pantalla. */
  private static final Map<String, List<String>> ESTADOS =
      Map.of(
          DEPOSITO,
          List.of(EN_DEPOSITO),
          EN_ASIGNADA,
          List.of("ASIGNACION_REALIZADA", "LISTA_PARA_ENTREGAR", "ENTREGA_FALLIDA"),
          EN_CAMINO,
          List.of("EN_TRASLADO", "EN_PUERTA"),
          EN_ENTREGADA,
          List.of("ENTREGADA"),
          "vencida",
          List.of("VENCIDA"));

  /**
   * Los órdenes que resuelve Donaciones: por fecha de registro. Vencen antes, por nombre y por
   * donante no los da el contrato de R11-01 y no se ofrecen.
   */
  private static final List<Criterio> CRITERIOS =
      List.of(
          new Criterio(
              "", "Registradas más recientes", "Primero, las registradas más recientemente."),
          new Criterio(
              "antigua", "Registradas más antiguas", "Primero, las registradas hace más tiempo."));

  private final DonacionesApi api;
  private final LocalDateTime ahora;
  private final AutoresDelHistorial autores;

  AdminOperacionDonaciones(DonacionesApi api, LocalDateTime ahora) {
    this(api, ahora, AutoresDelHistorial.con(null));
  }

  /** {@code autores} pone el nombre de la cuenta que hizo cada cambio del historial (R11-10). */
  AdminOperacionDonaciones(DonacionesApi api, LocalDateTime ahora, AutoresDelHistorial autores) {
    this.api = api;
    this.ahora = ahora;
    this.autores = autores;
  }

  AdminDonacionesVista vista(ConsultaDonaciones pedida) {
    ConsultaDonaciones consulta =
        new ConsultaDonaciones(
            pedida.estado(),
            pedida.pronto(),
            AdminDonacionesVista.elegido(pedida.orden(), CRITERIOS),
            pedida.pagina(),
            pedida.buscar(),
            pedida.categoria(),
            pedida.registrada(),
            pedida.donante(),
            pedida.donacion());
    // Donaciones no filtra por vencimiento: "vencen pronto" muestra las del depósito y dice cuántas
    // vencen, sin filtrar en el cliente.
    String estado = consulta.pronto() ? DEPOSITO : consulta.estado();
    DonacionesApi.Resumen resumen = api.resumen();
    Tramo<DonacionesApi.Donacion> pagina =
        Paginas.pedir(
            consulta.pagina(), POR_PAGINA, numero -> api.pagina(filtros(estado, consulta, numero)));
    Map<UUID, String> nombres = nombres(pagina.filas());
    List<Donacion> filas =
        pagina.filas().stream().map(d -> fila(d, nombre(d, nombres), null, null)).toList();
    return new AdminDonacionesVista(
        new Tramo<>(filas, pagina.total(), pagina.pagina(), pagina.paginas()),
        consulta,
        CRITERIOS,
        false,
        consulta.pronto() ? porVencer(resumen.porVencer()) : null,
        resumen(resumen),
        categorias(),
        donante(consulta.donante()),
        null,
        false);
  }

  /** Las categorías del catálogo, para la pastilla Categoría (R11-34). */
  private List<AdminDonacionesVista.Opcion> categorias() {
    return Objects.requireNonNullElse(api.categorias(), List.<DonacionesApi.Categoria>of()).stream()
        .filter(c -> c.id() != null && c.nombre() != null)
        .map(c -> new AdminDonacionesVista.Opcion(c.id().toString(), c.nombre()))
        .toList();
  }

  /**
   * El nombre del donante del filtro que llega desde su ficha (R11-34), en una lectura de a uno;
   * "el donante elegido" si no se encuentra; null si no hay filtro.
   */
  private String donante(String clave) {
    UUID id = uuid(clave);
    if (id == null) {
      return null;
    }
    MultiValueMap<String, String> lote = new LinkedMultiValueMap<>();
    lote.add("id", id.toString());
    lote.add("size", "1");
    return Objects.requireNonNullElse(
            api.personas(lote).getBody(), List.<DonacionesApi.Persona>of())
        .stream()
        .map(AdminOperacionDonaciones::nombre)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse("el donante elegido");
  }

  private static UUID uuid(String clave) {
    try {
      return clave == null || clave.isEmpty() ? null : UUID.fromString(clave);
    } catch (IllegalArgumentException noEsUnId) {
      return null;
    }
  }

  /** La donación por su id, con quién la donó, el depósito y la entidad; null si no existe. */
  Donacion detalle(String clave) {
    UUID id;
    try {
      id = UUID.fromString(clave == null ? "" : clave.strip());
    } catch (IllegalArgumentException noEsUnId) {
      return null;
    }
    DonacionesApi.Donacion d;
    try {
      d = api.donacionIndependiente(id);
    } catch (HttpClientErrorException.NotFound noEsta) {
      return null;
    }
    DonacionesApi.Original original = null;
    if (d.donacionOriginalId() != null) {
      try {
        original = api.donacion(d.donacionOriginalId());
      } catch (HttpClientErrorException.NotFound noEsta) {
        // Sin la original: el donante sale de quienDono y el depósito no se escribe.
      }
    }
    String entidad = null;
    if (d.entidadBeneficiariaId() != null) {
      try {
        DonacionesApi.Entidad e = api.entidad(d.entidadBeneficiariaId());
        entidad = e == null || e.juridica() == null ? null : e.juridica().razonSocial();
      } catch (HttpClientErrorException.NotFound noEsta) {
        // La entidad ya no está: el historial dice "la entidad".
      }
    }
    String donante = nombre(original == null ? null : persona(original));
    return fila(d, donante == null ? visible(d) : donante, deposito(original), entidad);
  }

  private MultiValueMap<String, String> filtros(
      String estado, ConsultaDonaciones consulta, int pagina) {
    MultiValueMap<String, String> filtros = new LinkedMultiValueMap<>();
    ESTADOS.getOrDefault(estado, List.of()).forEach(e -> filtros.add("estado", e));
    filtros.add("orden", "antigua".equals(consulta.orden()) ? "antiguas" : "recientes");
    // R11-34: la búsqueda, la categoría, el período y el donante, resueltos en Donaciones
    if (!consulta.buscar().isEmpty()) {
      filtros.add("q", consulta.buscar());
    }
    UUID categoria = uuid(consulta.categoria());
    if (categoria != null) {
      filtros.add("categoriaId", categoria.toString());
    }
    UUID donante = uuid(consulta.donante());
    if (donante != null) {
      filtros.add("donanteId", donante.toString());
    }
    LocalDate[] periodo = consulta.periodo(ahora.toLocalDate());
    if (periodo[0] != null) {
      filtros.add("desde", periodo[0].toString());
      filtros.add("hasta", periodo[1].toString());
    }
    filtros.add("page", String.valueOf(pagina));
    filtros.add("size", String.valueOf(POR_PAGINA));
    return filtros;
  }

  /**
   * Los nombres de quienes donaron las donaciones de la página, en una sola lectura por lote (el
   * admin los ve aunque el donante oculte su nombre).
   */
  private Map<UUID, String> nombres(List<DonacionesApi.Donacion> pagina) {
    List<String> ids =
        pagina.stream()
            .map(DonacionesApi.Donacion::quienDono)
            .filter(q -> q != null && q.donanteId() != null)
            .map(q -> q.donanteId().toString())
            .distinct()
            .toList();
    if (ids.isEmpty()) {
      return Map.of();
    }
    MultiValueMap<String, String> lote = new LinkedMultiValueMap<>();
    ids.forEach(id -> lote.add("id", id));
    lote.add("size", String.valueOf(POR_PAGINA));
    Map<UUID, String> nombres = new HashMap<>();
    Objects.requireNonNullElse(api.personas(lote).getBody(), List.<DonacionesApi.Persona>of())
        .stream()
        .filter(p -> p.id() != null)
        .forEach(p -> nombres.put(p.id(), nombre(p)));
    return nombres;
  }

  private static String nombre(DonacionesApi.Donacion d, Map<UUID, String> nombres) {
    String nombre =
        d.quienDono() == null || d.quienDono().donanteId() == null
            ? null
            : nombres.get(d.quienDono().donanteId());
    return nombre == null ? visible(d) : nombre;
  }

  private static String visible(DonacionesApi.Donacion d) {
    return d.quienDono() == null || d.quienDono().nombreVisible() == null
        ? "—"
        : d.quienDono().nombreVisible();
  }

  private static DonacionesApi.Persona persona(DonacionesApi.Original original) {
    return original.donante() == null ? null : original.donante().persona();
  }

  /** La razón social, o el nombre y el apellido; null si no hay persona. */
  private static String nombre(DonacionesApi.Persona persona) {
    if (persona == null) {
      return null;
    }
    if (persona.razonSocial() != null) {
      return persona.razonSocial();
    }
    if (persona.nombre() != null) {
      return persona.apellido() == null
          ? persona.nombre()
          : persona.nombre() + " " + persona.apellido();
    }
    return null;
  }

  private Donacion fila(DonacionesApi.Donacion d, String donante, String deposito, String entidad) {
    String estado = estado(Comun.normalizado(d.estadoActual()));
    List<DonacionesApi.Item> items = d.items() == null ? List.of() : d.items();
    String cantidad =
        items.size() > 1
            ? items.size() + " bienes"
            : Comun.cantidad(d.cantidad(), Comun.categoria(d));
    LocalDate vence =
        items.stream()
            .map(AdminOperacionDonaciones::fechaDeVencimiento)
            .filter(Objects::nonNull)
            .min(Comparator.naturalOrder())
            .orElse(null);
    return new Donacion(
        d.id().toString(),
        d.descripcion(),
        cantidad,
        cantidad.replace(" unidades", " u").replace(" unidad", " u"),
        donante,
        d.fechaRegistro() == null ? "" : DIA.format(d.fechaRegistro()),
        estado,
        vence(vence, DEPOSITO.equals(estado)),
        DEPOSITO.equals(estado)
            && vence != null
            && !vence.isBefore(ahora.toLocalDate())
            && !vence.isAfter(ahora.toLocalDate().plusDays(PRONTO)),
        items.isEmpty() ? "—" : estadoDelBien(items.getFirst()),
        deposito,
        entidad,
        false,
        items.size() > 1 ? items.stream().map(this::bien).toList() : null,
        historial(d, deposito, entidad));
  }

  private Bien bien(DonacionesApi.Item item) {
    DonacionesApi.BienNormalizado bien = item.bien();
    String que = bien == null || bien.bien() == null ? "" : bien.bien().descripcion();
    int cantidad = item.cantidad() == null ? 0 : item.cantidad();
    return new Bien(
        que,
        Comun.cantidad(cantidad, bien == null ? null : bien.categoria()),
        estadoDelBien(item),
        vence(fechaDeVencimiento(item), false));
  }

  private static LocalDate fechaDeVencimiento(DonacionesApi.Item item) {
    return item == null || item.bien() == null || item.bien().bien() == null
        ? null
        : item.bien().bien().fechaVencimiento();
  }

  /** "NUEVO" → "Nuevo". */
  private static String estadoDelBien(DonacionesApi.Item item) {
    String estado =
        item == null || item.bien() == null || item.bien().bien() == null
            ? null
            : item.bien().bien().estado();
    if (estado == null || estado.isBlank()) {
      return "—";
    }
    String minusculas = estado.toLowerCase(Locale.ROOT).replace('_', ' ');
    return Character.toUpperCase(minusculas.charAt(0)) + minusculas.substring(1);
  }

  /** "17 de octubre, en 2 días" si está en el depósito y todavía no venció; si no, la fecha. */
  private String vence(LocalDate fecha, boolean enDeposito) {
    if (fecha == null) {
      return null;
    }
    long dias = ChronoUnit.DAYS.between(ahora.toLocalDate(), fecha);
    if (!enDeposito || dias < 0) {
      return DIA.format(fecha);
    }
    if (dias == 0) {
      return DIA.format(fecha) + ", hoy";
    }
    return DIA.format(fecha) + (dias == 1 ? ", mañana" : ", en " + dias + " días");
  }

  /** La dirección del depósito de la original: "Av. Ejemplo 1234". */
  private static String deposito(DonacionesApi.Original original) {
    DonacionesApi.Direccion direccion = original == null ? null : original.deposito();
    if (direccion == null || direccion.calle() == null) {
      return null;
    }
    return direccion.altura() == null
        ? direccion.calle()
        : direccion.calle() + " " + direccion.altura();
  }

  private static String estado(String normalizado) {
    return switch (normalizado) {
      case ASIGNADA, LISTA, FALLIDA -> EN_ASIGNADA;
      case TRASLADO, PUERTA -> EN_CAMINO;
      case ENTREGADA -> EN_ENTREGADA;
      case VENCIDA -> "vencida";
      default -> DEPOSITO;
    };
  }

  /** Cada cambio de estado, del más viejo al más nuevo, con quién lo hizo si se sabe. */
  private List<String> historial(DonacionesApi.Donacion d, String deposito, String entidad) {
    List<String> historial = new ArrayList<>();
    String registrada = d.fechaRegistro() == null ? "" : DIA.format(d.fechaRegistro());
    historial.add(
        registrada + ": llegó al depósito" + (deposito == null ? "" : " de " + deposito) + ".");
    String hacia = entidad == null ? "la organización" : entidad;
    List<DonacionesApi.Cambio> cambios = d.historial() == null ? List.of() : d.historial();
    cambios.stream()
        .filter(c -> c.timestamp() != null && c.estadoAnterior() != null)
        .sorted(Comparator.comparing(DonacionesApi.Cambio::timestamp))
        .forEach(
            c -> {
              String dia = DIA.format(c.timestamp());
              String por = autores.de(c.actor());
              switch (Comun.normalizado(c.estadoNuevo())) {
                case ASIGNADA ->
                    historial.add(
                        dia
                            + ": quedó asignada a "
                            + hacia
                            + "."
                            + (por.isEmpty() ? "" : " La aprobó " + por + "."));
                case LISTA -> historial.add(dia + ": quedó lista para entregar.");
                case TRASLADO ->
                    historial.add(DIA_Y_HORA.format(c.timestamp()) + ": salió en camino.");
                case PUERTA ->
                    historial.add(DIA_Y_HORA.format(c.timestamp()) + ": llegó a " + hacia + ".");
                case ENTREGADA -> historial.add(dia + ": " + hacia + " confirmó que la recibió.");
                case FALLIDA -> historial.add(dia + ": la entrega no pudo llegar.");
                case VENCIDA ->
                    historial.add(
                        DIA_Y_HORA.format(c.timestamp())
                            + ": quedó vencida"
                            + (por.isEmpty() ? "" : "; la marcó " + por)
                            + ". Dejó de estar disponible para asignar.");
                case ENDEPOSITO -> historial.add(dia + ": volvió al depósito.");
                default -> {
                  // Un estado que la pantalla no conoce no se muestra.
                }
              }
            });
    return historial;
  }

  /** "7 en el depósito · 4 asignadas · 1 en camino · 9 entregas confirmadas en octubre. Al …". */
  private String resumen(DonacionesApi.Resumen resumen) {
    Map<String, Long> porEstado =
        resumen.donacionesPorEstado() == null ? Map.of() : resumen.donacionesPorEstado();
    long enDeposito = contar(porEstado, DEPOSITO);
    long asignadas = contar(porEstado, EN_ASIGNADA);
    long enCamino = contar(porEstado, EN_CAMINO);
    long entregadas =
        resumen.entregadasEnElMes() == null || resumen.entregadasEnElMes().cantidad() == null
            ? 0
            : resumen.entregadasEnElMes().cantidad();
    return numero(enDeposito)
        + " en el depósito · "
        + numero(asignadas)
        + (asignadas == 1 ? " asignada · " : " asignadas · ")
        + numero(enCamino)
        + " en camino · "
        + numero(entregadas)
        + (entregadas == 1 ? " entrega confirmada en " : " entregas confirmadas en ")
        + MES.format(ahora)
        + ". Al "
        + Comun.DIA_Y_ANIO.format(ahora)
        + ", "
        + HORA.format(ahora)
        + ".";
  }

  private static long contar(Map<String, Long> porEstado, String estado) {
    return ESTADOS.get(estado).stream()
        .map(porEstado::get)
        .filter(Objects::nonNull)
        .mapToLong(Long::longValue)
        .sum();
  }

  private static String numero(long valor) {
    return NumberFormat.getIntegerInstance(Comun.ES_AR).format(valor);
  }

  /**
   * Cuántas del depósito vencen en los próximos 7 días y cuál primero, porque la lista no se puede
   * filtrar por vencimiento.
   */
  private static String porVencer(DonacionesApi.PorVencer porVencer) {
    long cantidad = porVencer == null || porVencer.cantidad() == null ? 0 : porVencer.cantidad();
    if (cantidad == 0) {
      return "Ninguna donación del depósito vence en los próximos 7 días.";
    }
    String cuantas =
        cantidad == 1
            ? "1 donación del depósito vence en los próximos 7 días"
            : numero(cantidad) + " donaciones del depósito vencen en los próximos 7 días";
    DonacionesApi.Proxima proxima = porVencer.proxima();
    String primera =
        proxima == null || proxima.fechaVencimiento() == null
            ? ""
            : "; la primera"
                + (proxima.descripcion() == null ? "" : ", " + proxima.descripcion() + ",")
                + " el "
                + DIA.format(proxima.fechaVencimiento());
    return cuantas
        + primera
        + ". Esta lista todavía no se puede filtrar por vencimiento: te mostramos todas las del"
        + " depósito.";
  }
}
