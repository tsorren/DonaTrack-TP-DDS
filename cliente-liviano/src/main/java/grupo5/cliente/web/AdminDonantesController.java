package grupo5.cliente.web;

import grupo5.cliente.puertos.AdminPersonasPuerto;
import grupo5.cliente.vistas.AdminDonantesVista.Importacion;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Donantes del admin (RF-15 y RF-20) con los datos del puerto, y las pantallas que comparten su
 * hoja: la importación en curso, su resultado y el archivo que no se aceptó. Con {@code ?archivo=}
 * (fuera de la maqueta) la importación sale de Donaciones y la pantalla es la de su avance.
 */
@Controller
public class AdminDonantesController {

  private static final String DONANTES = "admin-donantes";
  private static final String MODELO = "adminDonantes";

  private final AdminPersonasPuerto personas;
  private final Pantallas pantallas;

  AdminDonantesController(AdminPersonasPuerto personas, Pantallas pantallas) {
    this.personas = personas;
    this.pantallas = pantallas;
  }

  @GetMapping("/" + DONANTES)
  String donantes(Model model) {
    String vista = pantallas.mostrar(DONANTES, model);
    model.addAttribute(MODELO, personas.donantes());
    return vista;
  }

  @GetMapping("/" + DONANTES + "-{accion:importando|importado|importar-error}")
  String importacion(
      @PathVariable String accion,
      @RequestParam(required = false) UUID archivo,
      @RequestParam(required = false) String nombre,
      Model model) {
    Importacion importacion = personas.importacion(archivo, nombre);
    if (importacion == null && !"importar-error".equals(accion)) {
      return "redirect:/" + DONANTES;
    }
    String pantalla = DONANTES + "-" + (archivo == null ? accion : importacion.accion());
    String vista = pantallas.mostrar(pantalla, model);
    model.addAttribute(MODELO, personas.donantes());
    model.addAttribute("importacion", importacion);
    return vista;
  }
}
