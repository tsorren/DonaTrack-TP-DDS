package grupo5.cliente.rest;

import static grupo5.cliente.rest.Comun.ARGENTINA;
import static grupo5.cliente.rest.Comun.DIA;
import static grupo5.cliente.rest.Comun.DIA_Y_HORA;

import grupo5.cliente.puertos.DonanteEntidadesPuerto;
import grupo5.cliente.rest.DonacionesApi.Categoria;
import grupo5.cliente.rest.DonacionesApi.EntidadParaDonante;
import grupo5.cliente.rest.DonacionesApi.Subcategoria;
import grupo5.cliente.vistas.DonanteEntidadesVista;
import grupo5.cliente.vistas.DonanteEntidadesVista.Entidad;
import grupo5.cliente.vistas.DonanteEntidadesVista.Pedido;
import grupo5.cliente.vistas.Tramo;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
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
 * Entidades del donante desde Donaciones (R11-12): una página de {@code
 * /api/donantes/{id}/entidades} con la búsqueda, la categoría y "Ya doné" resueltas en el servicio
 * sobre todas las activas (el total en {@code X-Total-Count} y, en cada una, si el donante ya le
 * donó), las necesidades vigentes de cada una y la categoría y la unidad de su subcategoría. Las
 * categorías del filtro y de los grupos salen del mismo catálogo de subcategorías. Lo recibido de
 * cada necesidad no lo da la API (HB-18): el renglón va sin barra.
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.donante", havingValue = "rest")
class DonanteEntidadesRest implements DonanteEntidadesPuerto {

  static final int POR_PAGINA = 6;

  private final DonacionesApi api;
  private final Clock reloj;

  DonanteEntidadesRest(DonacionesApi api, Clock reloj) {
    this.api = api;
    this.reloj = reloj;
  }

  @Override
  public DonanteEntidadesVista entidades(UUID donante, Consulta consulta) {
    Map<UUID, Subcategoria> subcategorias = subcategorias();
    List<DonanteEntidadesVista.Categoria> categorias = categorias(subcategorias);
    // Una categoría que no está entre las del filtro no se manda (no filtra)
    boolean conCategoria =
        categorias.stream().anyMatch(c -> c.valor().equals(consulta.categoria()));
    MultiValueMap<String, String> filtros = new LinkedMultiValueMap<>();
    if (!consulta.nombre().isEmpty()) {
      filtros.add("nombre", consulta.nombre());
    }
    if (conCategoria) {
      filtros.add("categoriaId", consulta.categoria());
    }
    if (consulta.soloDonadas()) {
      filtros.add("soloDonadas", "true");
    }
    int pagina = consulta.pagina();
    ResponseEntity<List<EntidadParaDonante>> respuesta = pedirPagina(donante, filtros, pagina);
    long total = total(respuesta);
    int paginas = Tramo.paginas(total, POR_PAGINA);
    if (pagina > paginas) {
      pagina = paginas;
      respuesta = pedirPagina(donante, filtros, pagina);
      total = total(respuesta);
      paginas = Tramo.paginas(total, POR_PAGINA);
    }
    List<EntidadParaDonante> lista = respuesta.getBody() == null ? List.of() : respuesta.getBody();
    List<Entidad> entidades =
        lista.stream()
            .map(
                e ->
                    new Entidad(
                        e.razonSocial(),
                        e.yaLeDono(),
                        api.necesidadesPublicas(e.id()).stream()
                            .map(
                                n ->
                                    pedido(
                                        n,
                                        n.idSubcategoria() == null
                                            ? null
                                            : subcategorias.get(n.idSubcategoria())))
                            .toList()))
            .toList();
    return new DonanteEntidadesVista(
        entidades,
        total,
        pagina,
        paginas,
        paginas,
        categorias,
        "hoy, " + LocalDateTime.now(reloj.withZone(ARGENTINA)).format(DIA_Y_HORA));
  }

  private ResponseEntity<List<EntidadParaDonante>> pedirPagina(
      UUID donante, MultiValueMap<String, String> filtros, int pagina) {
    MultiValueMap<String, String> conPagina = new LinkedMultiValueMap<>(filtros);
    conPagina.set("page", String.valueOf(pagina - 1));
    conPagina.set("size", String.valueOf(POR_PAGINA));
    return api.entidadesParaDonante(donante, conPagina);
  }

  /** El total de {@code X-Total-Count}; sin la cabecera, lo que vino en la página. */
  private static long total(ResponseEntity<List<EntidadParaDonante>> respuesta) {
    int enLaPagina = respuesta.getBody() == null ? 0 : respuesta.getBody().size();
    String total = respuesta.getHeaders().getFirst("X-Total-Count");
    try {
      return total == null ? enLaPagina : Long.parseLong(total);
    } catch (NumberFormatException malEscrito) {
      return enLaPagina;
    }
  }

  /** Las subcategorías por id; sin ellas, los pedidos quedan sin categoría ni unidad. */
  private Map<UUID, Subcategoria> subcategorias() {
    try {
      return api.subcategorias().stream()
          .filter(s -> s.id() != null)
          .collect(Collectors.toMap(Subcategoria::id, Function.identity(), (a, b) -> a));
    } catch (RestClientException sinSubcategorias) {
      return Map.of();
    }
  }

  /** Las categorías de las subcategorías, por nombre, con el ícono de cada una. */
  private static List<DonanteEntidadesVista.Categoria> categorias(
      Map<UUID, Subcategoria> subcategorias) {
    Map<UUID, Categoria> porId = new LinkedHashMap<>();
    subcategorias.values().stream()
        .map(Subcategoria::categoria)
        .filter(c -> c != null && c.id() != null && c.nombre() != null)
        .forEach(c -> porId.putIfAbsent(c.id(), c));
    return porId.values().stream()
        .sorted(Comparator.comparing(Categoria::nombre))
        .map(
            c ->
                new DonanteEntidadesVista.Categoria(
                    c.id().toString(), c.nombre(), Comun.icono(c.nombre())))
        .toList();
  }

  private static Pedido pedido(DonacionesApi.NecesidadPublica n, Subcategoria sub) {
    Categoria categoria = sub == null ? null : sub.categoria();
    String unidad = sub == null ? null : sub.unidad();
    if (unidad == null && categoria != null) {
      unidad = categoria.unidad();
    }
    boolean conCategoria =
        categoria != null && categoria.id() != null && categoria.nombre() != null;
    int cantidad = Objects.requireNonNullElse(n.cantidadNecesitada(), 0);
    return new Pedido(
        n.descripcion(),
        conCategoria ? categoria.id().toString() : null,
        conCategoria ? Comun.icono(categoria.nombre()) : null,
        "Piden " + Comun.cantidad(cantidad, new Categoria(null, null, unidad, null, null, null)),
        n.fechaFin() == null ? "Sin fecha de cierre" : "Hasta el " + n.fechaFin().format(DIA),
        null,
        cantidad);
  }
}
