package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.RecibidasPuerto;
import grupo5.cliente.vistas.RecibidasVista;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Donaciones recibidas de la entidad (RF-11), con los datos del puerto. Sin entidad todavía, manda
 * a ingresar, como un 401 de la API. Desde R11-43 los datos reales llegan de a una página: el
 * filtro por estado ({@code ?estado=}, repetible) lo resuelve Donaciones y la plantilla dibuja el
 * paginador; la maqueta manda todo y filtra la plantilla.
 */
@Controller
public class RecibidasController {

  /** Cada valor de {@code ?estado=} y su grupo; uno desconocido no filtra. */
  static final Map<String, String> GRUPOS =
      Map.of(
          "en-tu-puerta", RecibidasVista.PUERTA,
          "en-camino", RecibidasVista.CAMINO,
          "por-salir", RecibidasVista.SALIR,
          "con-problema", RecibidasVista.PROBLEMA,
          "recibidas", RecibidasVista.RECIBIDA);

  private final RecibidasPuerto recibidas;
  private final CuentaActual cuentas;
  private final Pantallas pantallas;

  RecibidasController(RecibidasPuerto recibidas, CuentaActual cuentas, Pantallas pantallas) {
    this.recibidas = recibidas;
    this.cuentas = cuentas;
    this.pantallas = pantallas;
  }

  @GetMapping("/entidad-recibidas")
  String recibidas(Model model, HttpServletRequest pedido) {
    String vista = pantallas.mostrar("entidad-recibidas", model);
    Optional<UUID> entidad = cuentas.entidad(pedido);
    if (entidad.isEmpty()) {
      return "redirect:/publico-ingresar";
    }
    RecibidasVista datos =
        recibidas.recibidas(entidad.get(), grupos(pedido), Paginadores.pedida(pedido));
    model.addAttribute("recibidas", datos);
    model.addAttribute("paginador", Paginadores.de(pedido, "Páginas de donaciones", datos.tramo()));
    return vista;
  }

  /** Los grupos pedidos en {@code ?estado=}: vacío si no se pidió ninguno que exista. */
  static Set<String> grupos(HttpServletRequest pedido) {
    String[] estados = pedido.getParameterValues("estado");
    if (estados == null) {
      return Set.of();
    }
    return Arrays.stream(estados)
        .map(GRUPOS::get)
        .filter(Objects::nonNull)
        .collect(Collectors.toUnmodifiableSet());
  }
}
