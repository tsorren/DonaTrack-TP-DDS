package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.AreaDeLaSesion;
import grupo5.cliente.seguridad.Area;
import grupo5.cliente.web.Pantallas;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * La sesión en la maqueta, que no tiene token: {@code ?escenario=con-sesion} en Contacto muestra la
 * cuenta del donante ({@code &area=entidad} o {@code &area=admin} cambia el rol), y una página de
 * error toma el área de la dirección que se pidió (el {@code /donante-inicio} de {@code
 * /donante-inicio?respuesta=403}). En lo público, {@code ?escenario=con-sesion} muestra además la
 * cuenta en el encabezado y {@code ?escenario=con-sesion-pendiente}, la de la entidad con la
 * solicitud en revisión (docs/interfaz/flujos/r11-p1-portada.md). Lo demás, sin sesión.
 */
@Component
@Profile("maqueta")
class AreaDeLaSesionMaqueta implements AreaDeLaSesion {

  private static final String CON_SESION = "con-sesion";
  private static final String CON_SESION_PENDIENTE = "con-sesion-pendiente";

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
    if (!CON_SESION_PENDIENTE.equals(pedido.getParameter("escenario"))) {
      return Optional.empty();
    }
    return Optional.of(
        Optional.ofNullable(pedido.getParameter("area"))
            .flatMap(Area::deRuta)
            .orElse(Area.ENTIDAD));
  }
}
