package grupo5.cliente.rest;

import static grupo5.cliente.rest.Comun.ARGENTINA;
import static grupo5.cliente.rest.Comun.DIA;
import static grupo5.cliente.rest.Comun.DIA_Y_ANIO;

import grupo5.cliente.puertos.PublicoEntidadesPuerto;
import grupo5.cliente.rest.DonacionesApi.Categoria;
import grupo5.cliente.rest.DonacionesApi.NecesidadPublica;
import grupo5.cliente.rest.DonacionesApi.Subcategoria;
import grupo5.cliente.vistas.PublicoEntidadesVista;
import grupo5.cliente.vistas.PublicoEntidadesVista.Entidad;
import grupo5.cliente.vistas.PublicoEntidadesVista.Pedido;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;

/**
 * Entidades y necesidades con los datos públicos de Donaciones: las entidades activas ({@code
 * /api/entidades/publicas}, el total en {@code X-Total-Count}) y todas las necesidades vigentes en
 * una sola llamada. Lo que la API pública no da no se muestra: el barrio, la misión, las
 * emergencias, lo recibido de cada necesidad (HB-18) y qué entidades tienen entregas en el mapa. La
 * categoría y la unidad salen de las subcategorías, que piden sesión: sin ella, los pedidos van sin
 * categoría.
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.publico", havingValue = "rest")
class PublicoEntidadesRest implements PublicoEntidadesPuerto {

  /** El máximo que da {@code /api/entidades/publicas} en una página. */
  private static final String POR_PAGINA = "100";

  private static final String OTRA = "Otra";

  private static final String NOTA =
      "Todavía no mostramos cuánto de cada pedido ya llegó, ni la misión de cada organización.";

  /**
   * Palabras del rubro (texto libre) que ubican a la entidad en uno de los grupos de la pantalla.
   */
  private static final List<String> TIPOS =
      List.of("Comedor", "Merendero", "Escuela", "Biblioteca", "Hogar", "Club");

  private final DonacionesApi api;
  private final Clock reloj;

  PublicoEntidadesRest(DonacionesApi api, Clock reloj) {
    this.api = api;
    this.reloj = reloj.withZone(ARGENTINA);
  }

  // ponytail: las primeras 100 entidades activas; con más, paginar por X-Total-Count.
  @Override
  public PublicoEntidadesVista entidades() {
    MultiValueMap<String, String> filtros = new LinkedMultiValueMap<>();
    filtros.add("page", "0");
    filtros.add("size", POR_PAGINA);
    ResponseEntity<List<DonacionesApi.EntidadPublica>> respuesta = api.entidadesPublicas(filtros);
    List<DonacionesApi.EntidadPublica> publicas =
        respuesta.getBody() == null ? List.of() : respuesta.getBody();
    Map<UUID, List<NecesidadPublica>> necesidades =
        api.necesidadesPublicas(null).stream()
            .filter(n -> n.idEntidad() != null)
            .collect(Collectors.groupingBy(NecesidadPublica::idEntidad));
    Map<UUID, Subcategoria> subcategorias = subcategorias();
    List<Entidad> entidades =
        publicas.stream()
            .filter(e -> necesidades.containsKey(e.id()))
            .map(e -> entidad(e, necesidades.get(e.id()), subcategorias))
            .toList();
    boolean conCategorias =
        entidades.stream().flatMap(e -> e.pedidos().stream()).anyMatch(p -> p.cat() != null);
    return new PublicoEntidadesVista(
        entidades,
        total(respuesta, publicas.size()),
        DIA_Y_ANIO.format(LocalDate.now(reloj)),
        null,
        conCategorias,
        false,
        NOTA);
  }

  private static Entidad entidad(
      DonacionesApi.EntidadPublica e,
      List<NecesidadPublica> necesidades,
      Map<UUID, Subcategoria> subcategorias) {
    List<Pedido> pedidos =
        necesidades.stream()
            .map(
                n ->
                    pedido(
                        n,
                        n.idSubcategoria() == null ? null : subcategorias.get(n.idSubcategoria())))
            .toList();
    String cat =
        pedidos.stream().map(Pedido::cat).filter(Objects::nonNull).findFirst().orElse(null);
    return new Entidad(
        e.id().toString(), tipo(e.rubro()), e.razonSocial(), cat, null, null, null, null, pedidos);
  }

  private static String tipo(String rubro) {
    String minusculas = rubro == null ? "" : rubro.toLowerCase(Locale.ROOT);
    return TIPOS.stream()
        .filter(t -> minusculas.contains(t.toLowerCase(Locale.ROOT)))
        .findFirst()
        .orElse(OTRA);
  }

  private static Pedido pedido(NecesidadPublica n, Subcategoria sub) {
    Categoria categoria = sub == null ? null : sub.categoria();
    String unidad = sub == null ? null : sub.unidad();
    if (unidad == null && categoria != null) {
      unidad = categoria.unidad();
    }
    String icono =
        categoria == null || categoria.nombre() == null ? null : Comun.icono(categoria.nombre());
    int cantidad = Objects.requireNonNullElse(n.cantidadNecesitada(), 0);
    boolean recurrente = "RECURRENTE".equals(n.tipo());
    String hasta = n.fechaFin() == null ? null : n.fechaFin().format(DIA);
    String cuando;
    if (recurrente) {
      cuando = hasta == null ? "Se repite" : "Se repite; este período termina el " + hasta;
    } else {
      cuando = hasta == null ? "Sin fecha de cierre" : "Hasta el " + hasta;
    }
    return new Pedido(
        n.descripcion(),
        icono == null ? null : icono.substring("cat-".length()),
        "Piden " + Comun.cantidad(cantidad, new Categoria(null, null, unidad, null, null, null)),
        cuando,
        null,
        cantidad,
        recurrente ? "recurrente" : "extraordinaria",
        false);
  }

  private static long total(ResponseEntity<?> respuesta, int enLaPagina) {
    String total = respuesta.getHeaders().getFirst("X-Total-Count");
    try {
      return total == null ? enLaPagina : Long.parseLong(total);
    } catch (NumberFormatException malEscrito) {
      return enLaPagina;
    }
  }

  /**
   * Las subcategorías por id. Piden sesión: un visitante recibe 401 y los pedidos van sin categoría
   * ni unidad, no una página de error.
   */
  private Map<UUID, Subcategoria> subcategorias() {
    try {
      return api.subcategorias().stream()
          .filter(s -> s.id() != null)
          .collect(Collectors.toMap(Subcategoria::id, Function.identity(), (a, b) -> a));
    } catch (RestClientException sinSesion) {
      return Map.of();
    }
  }
}
