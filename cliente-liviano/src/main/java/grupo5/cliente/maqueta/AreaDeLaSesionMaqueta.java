package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.AreaDeLaSesion;
import grupo5.cliente.seguridad.Area;
import grupo5.cliente.web.Pantallas;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * La sesión en la maqueta, que no tiene token: {@code ?escenario=con-sesion} en Contacto muestra la
 * cuenta del donante ({@code &area=entidad} o {@code &area=admin} cambia el rol), y una página de
 * error toma el área de la dirección que se pidió (el {@code /donante-inicio} de {@code
 * /donante-inicio?respuesta=403}). En lo público, {@code ?escenario=con-sesion} muestra además la
 * cuenta en el encabezado y {@code ?escenario=con-sesion-pendiente}, la de la entidad con la
 * solicitud en revisión (docs/interfaz/flujos/r11-p1-portada.md), igual que las pantallas de la
 * solicitud (r11-p2-pendiente-solicitud.md). Lo demás, sin sesión.
 */
@Component
@Profile("maqueta")
class AreaDeLaSesionMaqueta implements AreaDeLaSesion {

  private static final String CON_SESION = "con-sesion";
  private static final String CON_SESION_PENDIENTE = "con-sesion-pendiente";
  private static final String ENVIADA = "/publico-registro-entidad-enviada";
  private static final String CORREGIR = "/publico-registro-entidad-corregir";
  private static final Set<String> ESCENARIOS_DE_LA_SOLICITUD =
      Set.of("corregida", "sin-datos", "aprobada");

  @Override
  public Optional<Area> activa(HttpServletRequest pedido) {
    if (CON_SESION.equals(pedido.getParameter("escenario"))) {
      return Optional.of(
          Optional.ofNullable(pedido.getParameter("area"))
              .flatMap(Area::deRuta)
              .orElse(Area.DONANTE));
    }
    return Optional.ofNullable(pedido.getAttribute(RequestDispatcher.ERROR_REQUEST_URI))
        .map(String::valueOf)
        .filter(direccion -> direccion.matches("/" + Pantallas.NOMBRE))
        .flatMap(direccion -> Area.deRuta(direccion.substring(1, direccion.indexOf('-'))));
  }

  @Override
  public Optional<Area> pendiente(HttpServletRequest pedido) {
    if (!CON_SESION_PENDIENTE.equals(pedido.getParameter("escenario"))
        && !esDeLaSolicitud(pedido)) {
      return Optional.empty();
    }
    return Optional.of(
        Optional.ofNullable(pedido.getParameter("area"))
            .flatMap(Area::deRuta)
            .orElse(Area.ENTIDAD));
  }

  /**
   * Las pantallas de la solicitud (R11-41) se ven con la cuenta pendiente: Corregir siempre, y
   * "Recibimos la solicitud" en sus escenarios con datos (corregida, sin datos y aprobada).
   */
  private static boolean esDeLaSolicitud(HttpServletRequest pedido) {
    String ruta = pedido.getRequestURI();
    String escenario = pedido.getParameter("escenario");
    return CORREGIR.equals(ruta)
        || (ENVIADA.equals(ruta)
            && escenario != null
            && ESCENARIOS_DE_LA_SOLICITUD.contains(escenario));
  }
}
