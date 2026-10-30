package grupo5.cliente.web;

import grupo5.cliente.vistas.EntregaVista;
import grupo5.cliente.vistas.EstadoEntrega;
import grupo5.cliente.vistas.OpcionNavegacion;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Catálogo de componentes con todos sus estados (RF-CL-05). Los datos son de ejemplo. */
@Controller
class EstilosController {

  private static final String FOTO = "/img/ejemplo-entrega.svg";
  private static final String FOTO_ALT =
      "Cajas de alimentos apiladas junto a la puerta de un comedor";

  private static final List<EntregaVista> ENTREGAS =
      List.of(
          new EntregaVista(
              "Alimentos no perecederos",
              "Comedor Barrio Ejemplo",
              "18 de mayo de 2026",
              EstadoEntrega.EN_CAMINO,
              FOTO,
              FOTO_ALT,
              "#entrega-1"),
          new EntregaVista(
              "Ropa de abrigo",
              "Merendero Ejemplo Sur",
              "17 de mayo de 2026",
              EstadoEntrega.ENTREGADA,
              FOTO,
              FOTO_ALT,
              "#entrega-2"),
          new EntregaVista(
              "Útiles escolares",
              "Escuela Ejemplo 12",
              "16 de mayo de 2026",
              EstadoEntrega.CON_PROBLEMA,
              null,
              null,
              "#entrega-3"));

  private static final List<OpcionNavegacion> NAVEGACION =
      List.of(
          new OpcionNavegacion("Inicio", "#inicio", false),
          new OpcionNavegacion("Entregas a confirmar", "#entregas", true),
          new OpcionNavegacion("Necesidades", "#necesidades", false));

  @GetMapping("/estilos")
  String estilos(Model model) {
    model.addAttribute("entregas", ENTREGAS);
    model.addAttribute("estados", EstadoEntrega.values());
    model.addAttribute("opciones", NAVEGACION);
    return "paginas/estilos";
  }
}
