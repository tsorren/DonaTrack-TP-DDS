package grupo5.cliente.rest;

import static grupo5.cliente.rest.Comun.ASIGNADA;
import static grupo5.cliente.rest.Comun.DIA;
import static grupo5.cliente.rest.Comun.DIA_Y_HORA;
import static grupo5.cliente.rest.Comun.ENTREGADA;
import static grupo5.cliente.rest.Comun.FALLIDA;
import static grupo5.cliente.rest.Comun.LISTA;
import static grupo5.cliente.rest.Comun.TRASLADO;
import static grupo5.cliente.rest.Comun.VENCIDA;

import grupo5.cliente.vistas.AdminDonacionesVista;
import grupo5.cliente.vistas.AdminDonacionesVista.Bien;
import grupo5.cliente.vistas.AdminDonacionesVista.Donacion;
import java.text.Collator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
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
import java.util.function.Function;
import org.springframework.util.LinkedMultiValueMap;

/**
 * Las donaciones del admin desde Donaciones: el listado (con su historial), las originales (quién
 * donó y el depósito) y las entidades (a quién se asignó). Tres llamadas por pantalla, sin una por
 * donación. Lo usa {@link AdminOperacionRest}.
 */
final class AdminOperacionDonaciones {

  private static final String DEPOSITO = "deposito";
  private static final String ENDEPOSITO = "ENDEPOSITO";
  private static final int PRONTO = 7;
  private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMMM", Comun.ES_AR);
  private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm", Comun.ES_AR);
  private static final List<String> ORDEN_DE_ESTADOS =
      List.of(DEPOSITO, "asignada", "camino", "entregada", "vencida");

  private final DonacionesApi api;
  private final LocalDateTime ahora;
  private final Collator alfabetico = Collator.getInstance(Comun.ES_AR);

  AdminOperacionDonaciones(DonacionesApi api, LocalDateTime ahora) {
    this.api = api;
    this.ahora = ahora;
  }

  /**
   * Una donación lista para la pantalla, con las fechas para ordenar y contar: cuándo vence, cuándo
   * se registró y cuándo se entregó (null si no se entregó).
   */
  private record Fila(
      Donacion vista, LocalDate vence, LocalDateTime registro, LocalDateTime entregada) {}

  AdminDonacionesVista vista() {
    Map<UUID, DonacionesApi.Original> originales = new HashMap<>();
    api.originales().forEach(o -> originales.put(o.id(), o));
    Map<UUID, String> entidades = new HashMap<>();
    List<DonacionesApi.Entidad> lista = api.entidades(new LinkedMultiValueMap<>()).getBody();
    if (lista != null) {
      lista.stream()
          .filter(e -> e.juridica() != null)
          .forEach(e -> entidades.put(e.id(), e.juridica().razonSocial()));
    }
    List<Fila> filas =
        api.buscar(new LinkedMultiValueMap<>()).stream()
            .map(d -> fila(d, originales.get(d.donacionOriginalId()), entidades))
            .sorted(deDePartida())
            .toList();
    List<Donacion> todas = filas.stream().map(Fila::vista).toList();
    Comparator<Fila> porRegistro =
        Comparator.comparing(Fila::registro, Comparator.nullsFirst(Comparator.naturalOrder()));
    return new AdminDonacionesVista(
        todas,
        Map.of(
            "reciente", claves(filas, porRegistro.reversed()),
            "antigua", claves(filas, porRegistro),
            "nombre", claves(filas, alfabetico(f -> f.vista().que())),
            "donante", claves(filas, alfabetico(f -> f.vista().donante()))),
        resumen(todas, filas));
  }

  private Comparator<Fila> alfabetico(Function<Fila, String> texto) {
    return Comparator.comparing(f -> Objects.toString(texto.apply(f), ""), alfabetico);
  }

  private static List<String> claves(List<Fila> filas, Comparator<Fila> orden) {
    return filas.stream().sorted(orden).map(f -> f.vista().clave()).toList();
  }

  /**
   * Primero las del depósito que vencen antes, después el resto del depósito y los demás estados;
   * dentro de cada grupo, la registrada más recientemente primero.
   */
  private static Comparator<Fila> deDePartida() {
    return Comparator.<Fila>comparingInt(f -> ORDEN_DE_ESTADOS.indexOf(f.vista().estado()))
        .thenComparing(
            f -> DEPOSITO.equals(f.vista().estado()) ? f.vence() : null,
            Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(
            Fila::registro, Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder()));
  }

  private Fila fila(
      DonacionesApi.Donacion d, DonacionesApi.Original original, Map<UUID, String> entidades) {
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
    String deposito = deposito(original);
    String entidad = entidades.get(d.entidadBeneficiariaId());
    Donacion vista =
        new Donacion(
            d.id().toString(),
            d.descripcion(),
            cantidad,
            cantidad.replace(" unidades", " u").replace(" unidad", " u"),
            donante(d, original),
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
    return new Fila(
        vista,
        vence,
        d.fechaRegistro(),
        "entregada".equals(estado) ? Comun.desde(d, ENTREGADA) : null);
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

  /** Quién donó: la persona de la original (el admin la ve aunque el donante oculte su nombre). */
  private static String donante(DonacionesApi.Donacion d, DonacionesApi.Original original) {
    DonacionesApi.Persona persona =
        original == null || original.donante() == null ? null : original.donante().persona();
    if (persona != null && persona.razonSocial() != null) {
      return persona.razonSocial();
    }
    if (persona != null && persona.nombre() != null) {
      return persona.apellido() == null
          ? persona.nombre()
          : persona.nombre() + " " + persona.apellido();
    }
    return d.quienDono() == null || d.quienDono().nombreVisible() == null
        ? "—"
        : d.quienDono().nombreVisible();
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
      case ASIGNADA, LISTA, FALLIDA -> "asignada";
      case TRASLADO -> "camino";
      case ENTREGADA -> "entregada";
      case VENCIDA -> "vencida";
      default -> DEPOSITO;
    };
  }

  /** Cada cambio de estado, del más viejo al más nuevo, con quién lo hizo si se sabe. */
  private static List<String> historial(DonacionesApi.Donacion d, String deposito, String entidad) {
    List<String> historial = new ArrayList<>();
    String registrada = d.fechaRegistro() == null ? "" : DIA.format(d.fechaRegistro());
    historial.add(
        registrada + ": llegó al depósito" + (deposito == null ? "" : " de " + deposito) + ".");
    String hacia = entidad == null ? "la entidad" : entidad;
    List<DonacionesApi.Cambio> cambios = d.historial() == null ? List.of() : d.historial();
    cambios.stream()
        .filter(c -> c.timestamp() != null && c.estadoAnterior() != null)
        .sorted(Comparator.comparing(DonacionesApi.Cambio::timestamp))
        .forEach(
            c -> {
              String dia = DIA.format(c.timestamp());
              String por = c.actor() == null || c.actor().isBlank() ? "" : c.actor();
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
  private String resumen(List<Donacion> todas, List<Fila> filas) {
    long enDeposito = todas.stream().filter(d -> DEPOSITO.equals(d.estado())).count();
    long asignadas = todas.stream().filter(d -> "asignada".equals(d.estado())).count();
    long enCamino = todas.stream().filter(d -> "camino".equals(d.estado())).count();
    YearMonth mes = YearMonth.from(ahora);
    long entregadas =
        filas.stream()
            .filter(f -> f.entregada() != null && YearMonth.from(f.entregada()).equals(mes))
            .count();
    return enDeposito
        + " en el depósito · "
        + asignadas
        + (asignadas == 1 ? " asignada · " : " asignadas · ")
        + enCamino
        + " en camino · "
        + entregadas
        + (entregadas == 1 ? " entrega confirmada en " : " entregas confirmadas en ")
        + MES.format(mes)
        + ". Al "
        + Comun.DIA_Y_ANIO.format(ahora)
        + ", "
        + HORA.format(ahora)
        + ".";
  }
}
