package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.DonanteInicioPuerto;
import grupo5.cliente.vistas.DonanteAnioVista;
import grupo5.cliente.vistas.DonanteTableroVista;
import grupo5.cliente.vistas.Paginador;
import grupo5.cliente.vistas.Tramo;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.context.LazyContextVariable;

/**
 * El inicio del donante con los datos del puerto, en la versión de su tipo de cuenta. La empresa
 * lee {@code miInicio} ({@code donaciones} y {@code avisos} los define datos-donante, que sigue
 * envolviendo la pantalla); la persona (templates/persona/) lee {@code miAnio}, su año con las
 * cifras como números, sin la lista entera. En la empresa {@code miAnio} queda diferido: solo lo
 * lee la persona de ejemplo de la maqueta ({@code ?cuenta=persona}, que cambia la plantilla después
 * del controller), así que la empresa real no lo pide nunca. Sin cuenta, manda a ingresar.
 *
 * <p>El tablero de la empresa (R11-25, ficha r11-p2-tablero) lee la página de cada columna de su
 * propio parámetro ({@code tener}, {@code camino} y {@code llego}) y arma su paginador: cada flecha
 * cambia solo el suyo, conserva los otros y lleva el ancla de la columna (sin JavaScript, la
 * pantalla baja hasta ella). Con JavaScript, la columna manda {@code X-Tablero} con su nombre: solo
 * esa toma el foco, y la respuesta va sin el ancla, así HTMX no mueve la pantalla. Como cada
 * paginador se arma con la dirección de este pedido, la plantilla hace que HTMX renueve también los
 * pies de las otras dos columnas (hx-select-oob): sus enlaces no vuelven a una página vieja.
 */
@Controller
public class DonanteInicioController {

  /** La cabecera que manda la columna del tablero que cambió de página (hx-headers). */
  static final String COLUMNA = "X-Tablero";

  static final String TENER = "tener";
  static final String CAMINO = "camino";
  static final String LLEGO = "llego";

  private final DonanteInicioPuerto inicios;
  private final CuentaActual cuentas;
  private final Pantallas pantallas;

  DonanteInicioController(DonanteInicioPuerto inicios, CuentaActual cuentas, Pantallas pantallas) {
    this.inicios = inicios;
    this.cuentas = cuentas;
    this.pantallas = pantallas;
  }

  @GetMapping("/donante-inicio")
  String inicio(Model model, HttpServletRequest pedido) {
    Optional<UUID> donante = cuentas.donante(pedido);
    if (donante.isEmpty()) {
      return "redirect:/publico-ingresar";
    }
    UUID id = donante.get();
    String vista = pantallas.mostrarDonante("donante-inicio", id, model, pedido);
    if (vista.startsWith("persona/")) {
      model.addAttribute("miAnio", inicios.anio(id));
    } else {
      model.addAttribute("miInicio", inicios.inicio(id));
      DonanteTableroVista tablero =
          inicios.tablero(id, pedida(pedido, TENER), pedida(pedido, CAMINO), pedida(pedido, LLEGO));
      model.addAttribute("tablero", tablero);
      model.addAttribute(
          "paginasTener", paginador(pedido, TENER, "Para tener en cuenta", tablero.tener()));
      model.addAttribute(
          "paginasCamino", paginador(pedido, CAMINO, "En camino o por salir", tablero.camino()));
      model.addAttribute("paginasLlego", paginador(pedido, LLEGO, "Ya llegó", tablero.llego()));
      model.addAttribute("tableroParcial", pedido.getHeader(COLUMNA) != null);
      // "Volver a intentar" de una columna que no cargó: la misma dirección.
      model.addAttribute(
          "tableroDireccion",
          UriComponentsBuilder.fromPath(pedido.getRequestURI())
              .query(pedido.getQueryString())
              .build()
              .toUriString());
      model.addAttribute(
          "miAnio",
          new LazyContextVariable<DonanteAnioVista>() {
            @Override
            protected DonanteAnioVista loadValue() {
              return inicios.anio(id);
            }
          });
    }
    return vista;
  }

  private static int pedida(HttpServletRequest pedido, String columna) {
    return Tramo.pedida(pedido.getParameter(columna));
  }

  /** El paginador corto de una columna, o null con una sola página o si la columna no cargó. */
  private static Paginador paginador(
      HttpServletRequest pedido, String columna, String titulo, Tramo<?> tramo) {
    if (tramo == null) {
      return null;
    }
    return Paginador.de(
        "Páginas de " + titulo,
        tramo.pagina(),
        tramo.paginas(),
        pagina ->
            UriComponentsBuilder.fromPath(pedido.getRequestURI())
                .query(pedido.getQueryString())
                .replaceQueryParam(columna, pagina)
                .fragment("tablero-" + columna)
                .build()
                .toUriString(),
        columna.equals(pedido.getHeader(COLUMNA)));
  }
}
