package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.EntidadConfirmarPuerto;
import grupo5.cliente.puertos.RecibidasPuerto;
import grupo5.cliente.vistas.EntidadEntregaVista;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Confirmar recepción (RF-12) y las pantallas que usan su fragmento de entregas: el acta con
 * errores y Llegó con un problema, con las que esperan la confirmación; Recepción confirmada
 * (DA-06, la única respuesta con página propia), con la entrega ya confirmada y lo que quedó
 * asentado (las {@code -sellada-<clave>} son las respuestas de la maqueta) y las recibidas del mes,
 * que la plantilla cuenta como frutos, como Inicio. Sin entidad, manda a ingresar.
 */
@Controller
public class EntidadConfirmarController {

  private static final String SELLADA = "entidad-seguimiento-sellada-";

  private final EntidadConfirmarPuerto confirmar;
  private final RecibidasPuerto recibidas;
  private final CuentaActual cuentas;
  private final Pantallas pantallas;

  EntidadConfirmarController(
      EntidadConfirmarPuerto confirmar,
      RecibidasPuerto recibidas,
      CuentaActual cuentas,
      Pantallas pantallas) {
    this.confirmar = confirmar;
    this.recibidas = recibidas;
    this.cuentas = cuentas;
    this.pantallas = pantallas;
  }

  @GetMapping({
    "/entidad-seguimiento-confirmar",
    "/entidad-seguimiento-confirmar-error",
    "/entidad-seguimiento-problema"
  })
  String acta(Model model, HttpServletRequest pedido) {
    return mostrar(model, pedido, pedido.getParameter("entrega"), confirmar::entregas);
  }

  /**
   * La entrega es la de {@code ?entrega=} o, en las respuestas de la maqueta, la de la dirección
   * ({@code -sellada-ropa}); {@code ?problema=} es el motivo de la respuesta, que solo usa la
   * maqueta. {@code delMes} son las recibidas de la misma lectura que Inicio, y {@code
   * cuentaLaRecien} dice si es la respuesta al acta ({@code recien}, o las de la maqueta): la
   * plantilla suma la recién confirmada a los frutos si Donaciones todavía no la tiene como
   * entregada.
   */
  @GetMapping({
    "/entidad-seguimiento-sellada",
    "/entidad-seguimiento-sellada-lentejas",
    "/entidad-seguimiento-sellada-ropa"
  })
  String confirmada(Model model, HttpServletRequest pedido) {
    String pantalla = Entidad.pantalla(pedido);
    String pedida = pedido.getParameter("entrega");
    boolean respuesta = Boolean.TRUE.equals(model.getAttribute("recien"));
    if (pedida == null && pantalla.startsWith(SELLADA)) {
      pedida = pantalla.substring(SELLADA.length());
      respuesta = true;
    }
    String motivo = pedido.getParameter("problema");
    model.addAttribute("cuentaLaRecien", respuesta);
    cuentas
        .entidad(pedido)
        .ifPresent(entidad -> model.addAttribute("delMes", recibidas.recibidas(entidad)));
    return mostrar(
        model, pedido, pedida, (entidad, clave) -> confirmar.confirmada(entidad, clave, motivo));
  }

  private String mostrar(
      Model model,
      HttpServletRequest pedido,
      String pedida,
      BiFunction<UUID, String, List<EntidadEntregaVista>> entregas) {
    String vista = pantallas.mostrar(Entidad.pantalla(pedido), model);
    Optional<UUID> entidad = cuentas.entidad(pedido);
    if (entidad.isEmpty()) {
      return Entidad.INGRESAR;
    }
    model.addAttribute("entregasPorConfirmar", entregas.apply(entidad.get(), pedida));
    model.addAttribute("hoyActa", confirmar.hoy());
    return vista;
  }
}
