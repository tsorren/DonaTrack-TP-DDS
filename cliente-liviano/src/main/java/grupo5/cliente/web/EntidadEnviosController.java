package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.EntidadNotificacionesPuerto;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Los formularios de la entidad que guardan de verdad, fuera de la maqueta (SPEC-08): llaman al
 * puerto y terminan con {@link Envios#listo}. En la maqueta los mismos formularios los atiende
 * SitioController, que vuelve a su pantalla sin guardar. Sin entidad, mandan a ingresar.
 */
@Controller
@Profile("!maqueta")
public class EntidadEnviosController {

  private final CuentaActual cuentas;
  private final EntidadNotificacionesPuerto notificaciones;

  EntidadEnviosController(CuentaActual cuentas, EntidadNotificacionesPuerto notificaciones) {
    this.cuentas = cuentas;
    this.notificaciones = notificaciones;
  }

  /**
   * La bandeja: el tilde de una fila ({@code marcar}) o la de dejarla sin leer ({@code no-leer}), o
   * el botón de la cabecera con las elegidas (sin ninguna, todas).
   */
  @PostMapping("/entidad-notificaciones-leidas")
  String marcar(
      @RequestParam(required = false) List<UUID> elegidas,
      @RequestParam(required = false) UUID marcar,
      @RequestParam(name = "no-leer", required = false) UUID noLeer,
      HttpServletRequest pedido,
      RedirectAttributes atributos) {
    UUID entidad = cuentas.entidad(pedido).orElse(null);
    if (entidad == null) {
      return Entidad.INGRESAR;
    }
    if (noLeer != null) {
      notificaciones.dejarSinLeer(noLeer);
      return Envios.listo("entidad-notificaciones", "Dejaste la novedad sin leer.", atributos);
    }
    notificaciones.marcarLeidas(
        entidad, marcar != null ? List.of(marcar) : (elegidas == null ? List.of() : elegidas));
    return Envios.listo("entidad-notificaciones", "Marcaste las novedades como leídas.", atributos);
  }

  /**
   * Mi perfil todavía no guarda: {@code PUT /api/personas/{id}} reemplaza la persona entera y el
   * cuerpo compartido ({@code PersonaNueva}) no lleva el documento, el piso ni los teléfonos, que
   * se borrarían; "Qué es" y "Qué te avisamos" no tienen dónde guardarse. Vuelve sin cambiar nada y
   * lo dice.
   */
  @PostMapping("/entidad-perfil-guardado")
  String guardarPerfil(
      @RequestParam(required = false) String seccion, RedirectAttributes atributos) {
    return Envios.listo(
        "avisos".equals(seccion) ? "entidad-perfil#como-te-avisamos" : "entidad-perfil#datos",
        "Todavía no podemos guardar estos cambios desde acá: siguen como estaban. Si necesitás"
            + " cambiar algo, escribinos desde Contacto.",
        atributos);
  }
}
