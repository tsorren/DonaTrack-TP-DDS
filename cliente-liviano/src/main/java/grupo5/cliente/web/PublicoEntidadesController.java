package grupo5.cliente.web;

import grupo5.cliente.puertos.PublicoEntidadesPuerto;
import grupo5.cliente.vistas.ConsultaOrganizaciones;
import grupo5.cliente.vistas.Listado;
import grupo5.cliente.vistas.PublicoEntidadesVista;
import grupo5.cliente.vistas.PublicoEntidadesVista.Entidad;
import grupo5.cliente.vistas.Tramo;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Organizaciones y sus necesidades (RF-22; ola PUB1, PUB1-05), con las del puerto. Busca ({@code
 * ?buscar=}; {@code ?q=} sigue valiendo) y filtra ({@code ?categoria=}, {@code ?pedido=} y {@code
 * ?emergencia=si}) sobre todas las organizaciones; el filtro, el orden por tipo, la página de
 * {@value #POR_PAGINA} ({@code ?pagina=}) y los grupos los arma {@link ConsultaOrganizaciones}.
 * Atributos: {@code entidadesPublicas} (lo que da el puerto), {@code organizaciones} (la página,
 * cada una solo con sus necesidades que cumplen los filtros), {@code vigentes} (clave a la cantidad
 * de necesidades vigentes de cada organización, sin filtrar, para «Necesidades satisfechas»),
 * {@code grupos} (la página por tipo, en orden), {@code listado} (la barra del componente listado,
 * R11-B04: la categoría y "Emergencias" solo si la fuente los trae), {@code paginador} y, para los
 * textos sin resultados, {@code busqueda}, {@code categoriaElegida} y {@code filtrado}.
 */
@Controller
public class PublicoEntidadesController {

  static final int POR_PAGINA = 6;

  private static final String RECURRENTE = "recurrente";

  private static final List<Listado.Opcion> PEDIDOS =
      List.of(
          new Listado.Opcion("", "Todas las necesidades"),
          new Listado.Opcion(RECURRENTE, "Recurrentes, se repiten cada mes"),
          new Listado.Opcion("extraordinaria", "Extraordinarios, con fecha límite"));

  private final Pantallas pantallas;
  private final PublicoEntidadesPuerto entidades;

  PublicoEntidadesController(Pantallas pantallas, PublicoEntidadesPuerto entidades) {
    this.pantallas = pantallas;
    this.entidades = entidades;
  }

  @GetMapping("/publico-entidades")
  String entidades(
      @RequestParam(required = false) String buscar,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String categoria,
      @RequestParam(required = false) String pedido,
      @RequestParam(required = false) String emergencia,
      Model model,
      HttpServletRequest peticion) {
    String vista = pantallas.mostrar("publico-entidades", model);
    PublicoEntidadesVista datos = entidades.entidades();
    model.addAttribute("entidadesPublicas", datos);
    // R11-B04: la búsqueda se llama buscar en todas las listas; q sigue valiendo
    String texto = Objects.requireNonNullElse(buscar != null ? buscar : q, "");
    String cat =
        datos.conCategorias() && PublicoMapaController.esCategoria(categoria) ? categoria : "";
    String tipo = PEDIDOS.stream().anyMatch(o -> o.valor().equals(pedido)) ? pedido : "";
    boolean urgentes = datos.conEmergencias() && "si".equals(emergencia);
    ConsultaOrganizaciones consulta =
        new ConsultaOrganizaciones(texto, cat, tipo, urgentes, Paginadores.pedida(peticion));

    Tramo<Entidad> pagina = consulta.pagina(datos, POR_PAGINA);
    model.addAttribute("vigentes", ConsultaOrganizaciones.vigentes(datos));
    model.addAttribute("organizaciones", pagina);
    model.addAttribute("grupos", ConsultaOrganizaciones.porGrupo(pagina.filas()));
    model.addAttribute("busqueda", consulta.buscar());
    model.addAttribute(
        "categoriaElegida",
        PublicoMapaController.CATEGORIAS.stream()
            .filter(c -> !cat.isEmpty() && c.valor().equals(cat))
            .findFirst()
            .orElse(null));
    model.addAttribute("filtrado", consulta.filtrada());
    model.addAttribute("paginador", Paginadores.de(peticion, "Páginas de organizaciones", pagina));

    Listado.Armado barra =
        Listado.en("entidades", "/publico-entidades", "Buscar organizaciones")
            .buscar("Buscar por nombre", consulta.buscar(), "Por ejemplo: comedor.");
    if (datos.conCategorias()) {
      barra.categoria(cat, PublicoMapaController.CATEGORIAS);
    }
    barra.otra(new Listado.Eleccion("pedido", "Tipo de necesidad", tipo, PEDIDOS));
    if (datos.conEmergencias()) {
      barra.casilla("emergencia", "si", "Emergencias", urgentes);
    }
    model.addAttribute("listado", barra.sacar("/publico-entidades").listo());
    return vista;
  }
}
