package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.EntidadConfirmarPuerto;
import grupo5.cliente.puertos.RecibidasPuerto;
import grupo5.cliente.vistas.EntidadEntregaVista;
import grupo5.cliente.vistas.RecibidasVista;
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
 * asentado (las {@code -sellada-<clave>} son las respuestas de la maqueta) y los frutos del mes,
 * contados como en Inicio. Sin entidad, manda a ingresar.
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
   * ({@code -sellada-ropa}). Los frutos (D4) son las recibidas del mes en curso, de la misma
   * lectura que Inicio: {@code frutos}, cuántas, y {@code mesFrutos}, el mes ("octubre"). En la
   * respuesta al acta ({@code recien}, o las de la maqueta), la recién confirmada cuenta aunque
   * Donaciones todavía no la tenga como entregada: es el fruto que brota.
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
    String vista = mostrar(model, pedido, pedida, confirmar::confirmada);
    String confirmada = pedida;
    boolean recien = respuesta;
    cuentas
        .entidad(pedido)
        .ifPresent(
            entidad -> {
              RecibidasVista delMes = recibidas.recibidas(entidad);
              List<String> claves =
                  delMes.asignadas().stream()
                      .filter(r -> RecibidasVista.RECIBIDA.equals(r.grupo()))
                      .map(RecibidasVista.Recibida::clave)
                      .toList();
              boolean falta = recien && confirmada != null && !claves.contains(confirmada);
              model.addAttribute("frutos", claves.size() + (falta ? 1 : 0));
              model.addAttribute("mesFrutos", delMes.mes());
            });
    return vista;
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
