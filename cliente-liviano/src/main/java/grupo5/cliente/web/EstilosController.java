package grupo5.cliente.web;

import grupo5.cliente.vistas.CifraVista;
import grupo5.cliente.vistas.EntregaVista;
import grupo5.cliente.vistas.EstadoEntrega;
import grupo5.cliente.vistas.OpcionNavegacion;
import grupo5.cliente.vistas.PasoRecorrido;
import grupo5.cliente.vistas.PasoRecorrido.Avance;
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

  private static final CifraVista REALIZADAS =
      new CifraVista(
          "Donaciones realizadas",
          "7",
          "+3",
          "respecto al mes pasado",
          List.of(10, 18, 14, 26, 70, 40),
          "#tarjeta-titulo");

  private static final List<PasoRecorrido> PASOS =
      List.of(
          new PasoRecorrido("En depósito", "12 de octubre", Avance.HECHO),
          new PasoRecorrido("Asignada", "13 de octubre", Avance.HECHO),
          new PasoRecorrido("Lista para entregar", "14 de octubre", Avance.HECHO),
          new PasoRecorrido("En camino", "Hoy, 14:30", Avance.ACTUAL),
          new PasoRecorrido("Entregada", "Pendiente", Avance.PENDIENTE));

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
    model.addAttribute("pasos", PASOS);
    model.addAttribute("realizadas", REALIZADAS);
    return "paginas/estilos";
  }
}
