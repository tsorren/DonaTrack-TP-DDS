package grupo5.cliente.web;

import grupo5.cliente.puertos.PublicoEntidadesPuerto;
import grupo5.cliente.vistas.Listado;
import grupo5.cliente.vistas.PublicoEntidadesVista;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Entidades y necesidades (RF-22), con las del puerto. El atributo es {@code entidadesPublicas}; la
 * búsqueda ({@code ?buscar=}; {@code ?q=} sigue valiendo) y los filtros ({@code ?categoria=},
 * {@code ?pedido=} y {@code ?emergencia=si}) los aplica la plantilla sobre la lista. {@code
 * listado} es la barra del componente listado (R11-B04), con lo mismo ya elegido: la categoría y
 * "Emergencias" solo si la fuente los trae.
 */
@Controller
public class PublicoEntidadesController {

  private static final List<Listado.Opcion> PEDIDOS =
      List.of(
          new Listado.Opcion("", "Todas las necesidades"),
          new Listado.Opcion("recurrente", "Recurrentes, se repiten cada mes"),
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
      Model model) {
    String vista = pantallas.mostrar("publico-entidades", model);
    PublicoEntidadesVista datos = entidades.entidades();
    model.addAttribute("entidadesPublicas", datos);
    // R11-B04: la búsqueda se llama buscar en todas las listas; q sigue valiendo
    String texto = buscar != null ? buscar : (q != null ? q : "");
    Listado.Armado barra =
        Listado.en("entidades", "/publico-entidades", "Buscar organizaciones")
            .buscar("Buscar por nombre", texto.strip(), "Por ejemplo: comedor.");
    if (datos.conCategorias()) {
      barra.categoria(
          PublicoMapaController.esCategoria(categoria) ? categoria : "",
          PublicoMapaController.CATEGORIAS);
    }
    barra.otra(
        new Listado.Eleccion(
            "pedido",
            "Tipo de necesidad",
            PEDIDOS.stream().anyMatch(o -> o.valor().equals(pedido)) ? pedido : "",
            PEDIDOS));
    if (datos.conEmergencias()) {
      barra.casilla("emergencia", "si", "Emergencias", "si".equals(emergencia));
    }
    model.addAttribute("listado", barra.sacar("/publico-entidades").listo());
    return vista;
  }
}
