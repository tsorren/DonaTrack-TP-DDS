package grupo5.cliente.rest;

import static grupo5.cliente.rest.Comun.ARGENTINA;
import static grupo5.cliente.rest.Comun.DIA;
import static grupo5.cliente.rest.Comun.DIA_Y_HORA;

import grupo5.cliente.puertos.DonanteEntidadesPuerto;
import grupo5.cliente.rest.DonacionesApi.Categoria;
import grupo5.cliente.rest.DonacionesApi.Subcategoria;
import grupo5.cliente.vistas.DonanteEntidadesVista;
import grupo5.cliente.vistas.DonanteEntidadesVista.Entidad;
import grupo5.cliente.vistas.DonanteEntidadesVista.Pedido;
import java.time.Clock;
import java.time.LocalDateTime;
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
 * Entidades del donante desde Donaciones: una página de las públicas ({@code
 * /api/entidades/publicas}, el total en {@code X-Total-Count}), las necesidades vigentes de cada
 * una, la categoría y la unidad de su subcategoría, y si el donante ya le donó (sus donaciones con
 * esa entidad). Lo recibido de cada necesidad no lo da la API (HB-18): el renglón va sin barra.
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
  public DonanteEntidadesVista entidades(UUID donante, String nombre, int pagina) {
    MultiValueMap<String, String> filtros = new LinkedMultiValueMap<>();
    if (nombre != null && !nombre.isBlank()) {
      filtros.add("nombre", nombre.strip());
    }
    filtros.add("page", String.valueOf(pagina - 1));
    filtros.add("size", String.valueOf(POR_PAGINA));
    ResponseEntity<List<DonacionesApi.EntidadPublica>> respuesta = api.entidadesPublicas(filtros);
    List<DonacionesApi.EntidadPublica> publicas =
        respuesta.getBody() == null ? List.of() : respuesta.getBody();
    long total = total(respuesta, publicas.size());
    Map<UUID, Subcategoria> subcategorias = subcategorias();
    Map<UUID, List<String>> yaDonadas =
        api.donaciones(donante).stream()
            .filter(d -> d.entidadBeneficiariaId() != null)
            .collect(
                Collectors.groupingBy(
                    DonacionesApi.Donacion::entidadBeneficiariaId,
                    Collectors.mapping(d -> d.id().toString(), Collectors.toList())));
    List<Entidad> entidades =
        publicas.stream()
            .map(
                e ->
                    new Entidad(
                        e.razonSocial(),
                        yaDonadas.getOrDefault(e.id(), List.of()),
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
    int paginas = (int) Math.max(1, (total + POR_PAGINA - 1) / POR_PAGINA);
    return new DonanteEntidadesVista(
        entidades,
        total,
        paginas,
        paginas,
        "hoy, " + LocalDateTime.now(reloj.withZone(ARGENTINA)).format(DIA_Y_HORA));
  }

  private static long total(ResponseEntity<?> respuesta, int enLaPagina) {
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

  private static Pedido pedido(DonacionesApi.NecesidadPublica n, Subcategoria sub) {
    Categoria categoria = sub == null ? null : sub.categoria();
    String unidad = sub == null ? null : sub.unidad();
    if (unidad == null && categoria != null) {
      unidad = categoria.unidad();
    }
    String cat =
        categoria == null || categoria.nombre() == null ? null : Comun.icono(categoria.nombre());
    int cantidad = Objects.requireNonNullElse(n.cantidadNecesitada(), 0);
    return new Pedido(
        n.descripcion(),
        cat,
        "Piden " + Comun.cantidad(cantidad, new Categoria(null, null, unidad, null, null, null)),
        n.fechaFin() == null ? "Sin fecha de cierre" : "Hasta el " + n.fechaFin().format(DIA),
        null,
        cantidad);
  }
}
