package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.EntidadConfirmarPuerto;
import grupo5.cliente.puertos.EntidadNecesidadesPuerto;
import grupo5.cliente.puertos.EntidadNotificacionesPuerto;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Los formularios de la entidad que guardan de verdad, fuera de la maqueta (SPEC-08): llaman al
 * puerto y terminan con {@link Envios#listo}. En la maqueta los mismos formularios los atiende
 * SitioController, que vuelve a su pantalla sin guardar. Sin entidad, mandan a ingresar.
 */
@Controller
@Profile("!maqueta")
public class EntidadEnviosController {

  private static final String NECESIDADES = "entidad-necesidades";

  /** Las opciones de ¿Se repite?; "una" (una sola vez) no tiene periodicidad. */
  private static final Map<String, String> PERIODICIDADES =
      Map.of("semana", "SEMANAL", "quincena", "QUINCENAL", "mes", "MENSUAL");

  private final CuentaActual cuentas;
  private final EntidadNotificacionesPuerto notificaciones;
  private final EntidadConfirmarPuerto confirmar;
  private final EntidadNecesidadesPuerto necesidades;

  EntidadEnviosController(
      CuentaActual cuentas,
      EntidadNotificacionesPuerto notificaciones,
      EntidadConfirmarPuerto confirmar,
      EntidadNecesidadesPuerto necesidades) {
    this.cuentas = cuentas;
    this.notificaciones = notificaciones;
    this.confirmar = confirmar;
    this.necesidades = necesidades;
  }

  /**
   * Publicar otra necesidad: qué (la subcategoría), cuánto, si se repite, hasta cuándo y por qué.
   */
  @PostMapping("/entidad-necesidades-publicada")
  String publicarNecesidad(
      @RequestParam UUID que,
      @RequestParam int cuanto,
      @RequestParam String repite,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate hasta,
      @RequestParam String motivo,
      HttpServletRequest pedido,
      RedirectAttributes atributos) {
    UUID entidad = cuentas.entidad(pedido).orElse(null);
    if (entidad == null) {
      return Entidad.INGRESAR;
    }
    necesidades.publicar(entidad, que, cuanto, PERIODICIDADES.get(repite), hasta, motivo.strip());
    return Envios.listo(
        NECESIDADES,
        "Publicamos tu necesidad. Ya la ven los donantes y el catálogo público.",
        atributos);
  }

  /**
   * Cambiar cantidad o motivo, con la versión leída. Si otra persona la cambió (409), vuelve a esa
   * necesidad abierta, con el aviso, para revisarla.
   */
  @PostMapping("/entidad-necesidades-cambiada")
  String cambiarNecesidad(
      @RequestParam UUID necesidad,
      @RequestParam long version,
      @RequestParam int cuanto,
      @RequestParam String motivo,
      HttpServletRequest pedido,
      RedirectAttributes atributos) {
    if (cuentas.entidad(pedido).isEmpty()) {
      return Entidad.INGRESAR;
    }
    String clave = "necesidad-" + necesidad;
    try {
      necesidades.cambiar(necesidad, version, cuanto, motivo.strip());
    } catch (HttpClientErrorException.Conflict otraPersona) {
      return Envios.listo(
          NECESIDADES + "?necesidad=" + clave + "#" + clave,
          "Otra persona de tu organización cambió esta necesidad mientras la editabas. Revisala y, si"
              + " hace falta, guardá tus cambios de nuevo.",
          atributos);
    }
    return Envios.listo(
        NECESIDADES + "?necesidad=" + clave + "#" + clave,
        "Guardamos los cambios de tu necesidad.",
        atributos);
  }

  @PostMapping("/entidad-necesidades-baja")
  String darDeBajaNecesidad(
      @RequestParam UUID necesidad, HttpServletRequest pedido, RedirectAttributes atributos) {
    if (cuentas.entidad(pedido).isEmpty()) {
      return Entidad.INGRESAR;
    }
    necesidades.darDeBaja(necesidad);
    return Envios.listo(
        NECESIDADES, "Diste de baja tu necesidad. Ya no la ven los donantes.", atributos);
  }

  /**
   * El acta de Confirmar recepción y de Llegó con un problema ({@code motivo}): sube las fotos y
   * deja la entrega como entregada, con el remito y el problema si lo hay. La clave es la entrega
   * de Logística; vuelve a Seguir entregas con el aviso.
   */
  @PostMapping("/entidad-seguimiento-sellada-{clave}")
  String confirmarRecepcion(
      @PathVariable UUID clave,
      @RequestParam List<MultipartFile> fotos,
      @RequestParam String remito,
      @RequestParam(required = false) String motivo,
      @RequestParam(required = false) Double cantidadRecibida,
      @RequestParam(required = false) String detalle,
      HttpServletRequest pedido,
      RedirectAttributes atributos) {
    UUID entidad = cuentas.entidad(pedido).orElse(null);
    if (entidad == null) {
      return Entidad.INGRESAR;
    }
    String elegido = motivo != null && motivo.matches("incompleta|danada|otro") ? motivo : null;
    confirmar.confirmar(
        entidad,
        clave.toString(),
        new EntidadConfirmarPuerto.Acta(fotos, remito, elegido, cantidadRecibida, detalle));
    return Envios.listo(
        "entidad-seguimiento",
        elegido == null
            ? "Confirmaste la recepción. Le avisamos a quien donó."
            : "Confirmaste la recepción y contaste el problema. Le avisamos a quien donó.",
        atributos);
  }

  /**
   * La bandeja: el tilde de una fila ({@code marcar}) o la de dejarla sin leer ({@code no-leer}), o
   * el botón de la cabecera con las elegidas (sin ninguna, todas). Vuelve a la misma página, con
   * sus filtros (R11-14).
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
    String volver = BandejaDeNotificaciones.volver("entidad-notificaciones", pedido);
    if (noLeer != null) {
      notificaciones.dejarSinLeer(noLeer);
      return Envios.listo(volver, "Dejaste la novedad sin leer.", atributos);
    }
    notificaciones.marcarLeidas(
        entidad, marcar != null ? List.of(marcar) : (elegidas == null ? List.of() : elegidas));
    return Envios.listo(volver, "Marcaste las novedades como leídas.", atributos);
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
