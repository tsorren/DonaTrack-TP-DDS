package grupo5.cliente.rest;

import grupo5.cliente.rest.DonacionesApi.Cambio;
import grupo5.cliente.rest.DonacionesApi.Categoria;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

/**
 * Lo común de los adaptadores REST que leen donaciones (Mis donaciones, Donaciones recibidas y las
 * pantallas del admin): los formatos de fecha de la Argentina, los estados de Donaciones
 * normalizados, la categoría, la cantidad con su unidad y el ícono del sprite. Lo comparten varios
 * frentes de la conexión de pantallas (SPEC-08): lo declara el esqueleto y nadie lo cambia.
 */
final class Comun {

  static final Locale ES_AR = Locale.forLanguageTag("es-AR");
  static final ZoneId ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");
  static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("d 'de' MMMM", ES_AR);
  static final DateTimeFormatter DIA_Y_ANIO =
      DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", ES_AR);
  static final DateTimeFormatter DIA_Y_HORA =
      DateTimeFormatter.ofPattern("d 'de' MMMM, H:mm", ES_AR);
  static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("H:mm", ES_AR);

  static final String ENTREGADA = "ENTREGADA";
  static final String FALLIDA = "ENTREGAFALLIDA";
  static final String VENCIDA = "VENCIDA";
  static final String TRASLADO = "ENTRASLADO";
  static final String ASIGNADA = "ASIGNACIONREALIZADA";
  static final String LISTA = "LISTAPARAENTREGAR";

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

  /** La categoría del primer ítem, o null si no la tiene completa. */
  static Categoria categoria(DonacionesApi.Donacion d) {
    Categoria categoria =
        d.items() == null
                || d.items().isEmpty()
                || d.items().getFirst() == null
                || d.items().getFirst().bien() == null
            ? null
            : d.items().getFirst().bien().categoria();
    return categoria == null || categoria.id() == null || categoria.nombre() == null
        ? null
        : categoria;
  }

  /** "EnDeposito" y "EN_DEPOSITO" (el enum del contrato) quedan iguales: "ENDEPOSITO". */
  static String normalizado(String estado) {
    return estado == null ? "" : estado.replace("_", "").toUpperCase(Locale.ROOT);
  }

  /** Cuándo entró al estado en el que está: su último cambio a ese estado, o el registro. */
  static LocalDateTime desde(DonacionesApi.Donacion d, String estado) {
    return d.historial() == null
        ? d.fechaRegistro()
        : d.historial().stream()
            .filter(c -> normalizado(c.estadoNuevo()).equals(estado))
            .map(Cambio::timestamp)
            .max(Comparator.naturalOrder())
            .orElse(d.fechaRegistro());
  }

  static String cantidad(int cantidad, Categoria categoria) {
    String unidad =
        categoria == null || categoria.unidad() == null ? null : UNIDADES.get(categoria.unidad());
    if (cantidad == 1 && "unidades".equals(unidad)) {
      unidad = "unidad";
    }
    return unidad == null ? String.valueOf(cantidad) : cantidad + " " + unidad;
  }

  static String icono(String nombre) {
    String minusculas = nombre.toLowerCase(Locale.ROOT);
    return ICONOS.entrySet().stream()
        .filter(e -> minusculas.contains(e.getKey()))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElse(null);
  }

  private Comun() {}
}
