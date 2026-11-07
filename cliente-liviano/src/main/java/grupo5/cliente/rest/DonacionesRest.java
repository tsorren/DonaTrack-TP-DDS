package grupo5.cliente.rest;

import grupo5.cliente.puertos.DonacionesPuerto;
import grupo5.cliente.rest.DonacionesApi.Cambio;
import grupo5.cliente.rest.DonacionesApi.Categoria;
import grupo5.cliente.vistas.EstadoEntrega;
import grupo5.cliente.vistas.MisDonacionesVista;
import grupo5.cliente.vistas.MisDonacionesVista.Donacion;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Mis donaciones desde Donaciones: la lista del donante, las categorías del filtro y, por cada
 * entidad distinta, su nombre para el destino. El número de cada donación es su posición, de la
 * primera a la última.
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.donaciones", havingValue = "rest")
class DonacionesRest implements DonacionesPuerto {

  private static final Locale ES_AR = Locale.forLanguageTag("es-AR");
  private static final ZoneId ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");
  private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("d 'de' MMMM", ES_AR);
  private static final DateTimeFormatter DIA_Y_ANIO =
      DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", ES_AR);
  private static final DateTimeFormatter DIA_Y_HORA =
      DateTimeFormatter.ofPattern("d 'de' MMMM, HH:mm", ES_AR);

  /** El sprite de categorías tiene cuatro íconos; una categoría sin el suyo no muestra ninguno. */
  private static final Map<String, String> ICONOS =
      Map.of(
          "aliment", "cat-alimentos",
          "ropa", "cat-ropa",
          "vestim", "cat-ropa",
          "escolar", "cat-escolares",
          "mueble", "cat-muebles");

  private static final Map<String, String> UNIDADES =
      Map.of(
          "KILOGRAMO", "kg",
          "LITROS", "l",
          "UNIDADES", "unidades",
          "METRO", "m",
          "METRO_CUADRADO", "m²",
          "METRO_CUBICO", "m³");

  private final DonacionesApi api;
  private final Clock reloj;

  DonacionesRest(DonacionesApi api, Clock reloj) {
    this.api = api;
    this.reloj = reloj;
  }

  @Override
  public MisDonacionesVista misDonaciones(UUID donante) {
    List<DonacionesApi.Donacion> lista =
        api.donaciones(donante).stream()
            .sorted(Comparator.comparing(DonacionesApi.Donacion::fechaRegistro).reversed())
            .toList();
    // HashMap y no toMap: una entidad sin nombre queda en null, sin NullPointerException.
    Map<UUID, String> entidades = new HashMap<>();
    lista.stream()
        .map(DonacionesApi.Donacion::entidadBeneficiariaId)
        .filter(Objects::nonNull)
        .distinct()
        .forEach(id -> entidades.put(id, nombre(api.entidad(id))));
    List<Donacion> donaciones =
        IntStream.range(0, lista.size())
            .mapToObj(i -> donacion(lista.get(i), lista.size() - i, entidades))
            .toList();
    List<MisDonacionesVista.Categoria> categorias =
        api.categorias().stream()
            .map(c -> new MisDonacionesVista.Categoria(c.id().toString(), c.nombre()))
            .toList();
    return new MisDonacionesVista(
        donaciones,
        categorias,
        "hoy, " + LocalDateTime.now(reloj.withZone(ARGENTINA)).format(DIA_Y_HORA));
  }

  private static Donacion donacion(
      DonacionesApi.Donacion d, long numero, Map<UUID, String> entidades) {
    Categoria categoria =
        d.items() == null || d.items().isEmpty() || d.items().getFirst().bien() == null
            ? null
            : d.items().getFirst().bien().categoria();
    String estado = normalizado(d.estadoActual());
    return new Donacion(
        d.id().toString(),
        numero,
        d.descripcion(),
        cantidad(d.cantidad(), categoria),
        categoria == null ? null : categoria.id().toString(),
        categoria == null ? null : icono(categoria.nombre()),
        categoria == null ? null : categoria.nombre(),
        entidades.get(d.entidadBeneficiariaId()),
        d.fechaRegistro().format(DIA_Y_ANIO),
        estadoEntrega(estado),
        nota(estado, desde(d, estado)));
  }

  /** "EnDeposito" y "EN_DEPOSITO" (el enum del contrato) quedan iguales: "ENDEPOSITO". */
  private static String normalizado(String estado) {
    return estado == null ? "" : estado.replace("_", "").toUpperCase(Locale.ROOT);
  }

  private static EstadoEntrega estadoEntrega(String estado) {
    return switch (estado) {
      case "ENTRASLADO" -> EstadoEntrega.EN_CAMINO;
      case "ENTREGADA" -> EstadoEntrega.ENTREGADA;
      case "ENTREGAFALLIDA", "VENCIDA" -> EstadoEntrega.CON_PROBLEMA;
      default -> EstadoEntrega.PENDIENTE;
    };
  }

  /** Cuándo entró al estado en el que está: su último cambio a ese estado, o el registro. */
  private static LocalDateTime desde(DonacionesApi.Donacion d, String estado) {
    return d.historial() == null
        ? d.fechaRegistro()
        : d.historial().stream()
            .filter(c -> normalizado(c.estadoNuevo()).equals(estado))
            .map(Cambio::timestamp)
            .max(Comparator.naturalOrder())
            .orElse(d.fechaRegistro());
  }

  private static String nota(String estado, LocalDateTime desde) {
    return switch (estado) {
      case "ASIGNACIONREALIZADA" -> "Asignada, espera el camión";
      case "LISTAPARAENTREGAR" -> "Lista para entregar, espera el camión";
      case "ENTRASLADO" -> "Salió el " + desde.format(DIA_Y_HORA);
      case "ENTREGADA" -> "Entrega confirmada el " + desde.format(DIA);
      case "ENTREGAFALLIDA" -> "No pudo llegar el " + desde.format(DIA);
      case "VENCIDA" -> "Venció en el depósito el " + desde.format(DIA);
      default -> "En el depósito, espera destino";
    };
  }

  private static String nombre(DonacionesApi.Entidad entidad) {
    return entidad == null || entidad.juridica() == null ? null : entidad.juridica().razonSocial();
  }

  private static String cantidad(int cantidad, Categoria categoria) {
    String unidad = categoria == null ? null : UNIDADES.get(categoria.unidad());
    if (cantidad == 1 && "unidades".equals(unidad)) {
      unidad = "unidad";
    }
    return unidad == null ? String.valueOf(cantidad) : cantidad + " " + unidad;
  }

  private static String icono(String nombre) {
    String minusculas = nombre.toLowerCase(Locale.ROOT);
    return ICONOS.entrySet().stream()
        .filter(e -> minusculas.contains(e.getKey()))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElse(null);
  }
}
