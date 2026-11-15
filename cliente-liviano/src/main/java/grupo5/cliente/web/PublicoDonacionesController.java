package grupo5.cliente.web;

import grupo5.cliente.puertos.PublicoPuerto;
import grupo5.cliente.vistas.Listado;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Entregas confirmadas (RF-01), con las del puerto. El período ({@code ?periodo=semana}; cualquier
 * otro valor es el último mes) lo elige la plantilla sobre la lista del mes. {@code listado} es la
 * barra del componente listado (R11-B04): el período queda como desplegable propio, porque el
 * puerto trae el mes y la semana, no un rango de fechas.
 */
@Controller
public class PublicoDonacionesController {

  static final String SEMANA = "semana";

  private static final List<Listado.Opcion> PERIODOS =
      List.of(new Listado.Opcion("", "Último mes"), new Listado.Opcion(SEMANA, "Última semana"));

  private final Pantallas pantallas;
  private final PublicoPuerto publico;

  PublicoDonacionesController(Pantallas pantallas, PublicoPuerto publico) {
    this.pantallas = pantallas;
    this.publico = publico;
  }

  @GetMapping("/publico-donaciones")
  String donaciones(@RequestParam(required = false) String periodo, Model model) {
    String vista = pantallas.mostrar("publico-donaciones", model);
    model.addAttribute("entregasConfirmadas", publico.entregasConfirmadas());
    model.addAttribute(
        "listado",
        Listado.en("entregas", "/publico-donaciones", "Período")
            .otra(
                new Listado.Eleccion(
                    "periodo", "Período", SEMANA.equals(periodo) ? SEMANA : "", PERIODOS))
            .sacar("/publico-donaciones")
            .listo());
    return vista;
  }
}
