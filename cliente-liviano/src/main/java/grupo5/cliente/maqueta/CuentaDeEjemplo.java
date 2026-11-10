package grupo5.cliente.maqueta;

import grupo5.cliente.vistas.CuentaVista;
import grupo5.cliente.vistas.OpcionNavegacion;
import grupo5.cliente.web.Pantallas;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * La segunda cuenta de ejemplo del donante: {@code ?cuenta=persona} la elige y queda en la sesión,
 * {@code ?cuenta=empresa} vuelve a la empresa. La persona tiene Misiones en el menú (RF-07); sus
 * pantallas viven en templates/persona/, y las que faltan le muestran un aviso
 * (docs/interfaz/flujos/raices-misiones.md). Entidades es común: la empresa y la persona la
 * recorren.
 */
final class CuentaDeEjemplo implements HandlerInterceptor {

  private static final String CUENTA = "cuenta";
  private static final String PERSONA = "persona";
  private static final String PREFIJO = "/donante-";
  private static final String MISIONES = PREFIJO + "misiones";
  private static final String ENTIDADES = PREFIJO + "entidades";
  // Las pantallas con versión propia en templates/persona/. Lista fija: la vista sale de esta
  // lista y no de la URL del pedido.
  private static final Set<String> PANTALLAS_PERSONA =
      Set.of(
          "donante-donaciones",
          "donante-donaciones-detalle",
          "donante-entidades",
          "donante-inicio",
          "donante-notificaciones",
          "donante-notificaciones-leidas",
          "donante-perfil",
          "donante-perfil-guardado",
          "donante-seguimiento");
  private static final CuentaVista CUENTA_PERSONA =
      new CuentaVista("Martina Ruiz", "Persona donante");

  @Override
  public void postHandle(
      HttpServletRequest pedido,
      HttpServletResponse respuesta,
      Object controller,
      ModelAndView vista) {
    String ruta = pedido.getRequestURI();
    // Los POST vuelven con una redirección, sin menú: no hay nada que cambiar.
    if (vista == null
        || vista.getModel().get("navegacion") == null
        || !ruta.substring(1).matches(Pantallas.NOMBRE)
        || !ruta.startsWith(PREFIJO)
        || !esPersona(pedido)) {
      return;
    }
    List<OpcionNavegacion> menu = conMisiones(vista.getModel().get("navegacion"), ruta);
    String seccion =
        menu.stream()
            .filter(OpcionNavegacion::actual)
            .findFirst()
            .map(OpcionNavegacion::texto)
            .orElse((String) vista.getModel().get("seccion"));
    vista.addObject("navegacion", menu).addObject(CUENTA, CUENTA_PERSONA).addObject(PERSONA, true);
    vista.addObject("seccion", seccion);
    if (MISIONES.equals(ruta)) {
      return;
    }
    String pantalla =
        PANTALLAS_PERSONA.stream().filter(ruta.substring(1)::equals).findFirst().orElse(null);
    if (pantalla != null) {
      vista.setViewName(PERSONA + "/" + pantalla);
    } else if (!ENTIDADES.equals(ruta)) {
      vista.addObject("seccion", seccion == null ? "Notificaciones" : seccion);
      vista.setViewName("aviso-persona");
    }
  }

  private static boolean esPersona(HttpServletRequest pedido) {
    String elegida = pedido.getParameter(CUENTA);
    if (PERSONA.equals(elegida) || "empresa".equals(elegida)) {
      pedido.getSession().setAttribute(CUENTA, elegida);
    }
    return PERSONA.equals(pedido.getSession().getAttribute(CUENTA));
  }

  /** El menú de la empresa con Misiones antes de Seguimiento, la última sección (§6.2). */
  private static List<OpcionNavegacion> conMisiones(Object empresa, String ruta) {
    List<OpcionNavegacion> menu =
        new ArrayList<>(((List<?>) empresa).stream().map(OpcionNavegacion.class::cast).toList());
    var misiones = new OpcionNavegacion("Misiones", "Misiones", "incentivos", MISIONES, false);
    menu.add(menu.size() - 1, misiones);
    return menu.stream().map(opcion -> marcada(opcion, ruta)).toList();
  }

  private static OpcionNavegacion marcada(OpcionNavegacion o, String ruta) {
    boolean actual = ruta.equals(o.ruta()) || ruta.startsWith(o.ruta() + "-");
    return new OpcionNavegacion(o.texto(), o.textoCorto(), o.icono(), o.ruta(), actual);
  }
}
