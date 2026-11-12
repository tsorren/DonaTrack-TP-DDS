package grupo5.cliente.web;

import grupo5.cliente.puertos.AdministradorActual;
import grupo5.cliente.puertos.CuentasDelMenu;
import grupo5.cliente.puertos.DirectorioDeAdministradores;
import grupo5.cliente.puertos.DirectorioDeAdministradores.Administrador;
import jakarta.servlet.http.HttpServletRequest;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Mi perfil de la administración (R11-10, ficha docs/interfaz/flujos/r11-p1-admins.md §4.1): el
 * nombre y el apellido de la cuenta de la sesión, que es el que queda en lo que aprueba. El correo
 * no se cambia desde acá. Si el proveedor de ingreso no responde, la pantalla lo dice con el código
 * del pedido y, al guardar, conserva lo escrito. El atributo es {@code miPerfil}: {@code perfil} ya
 * es el enlace del layout.
 */
@Controller
public class AdminPerfilController {

  static final String PANTALLA = "admin-perfil";
  static final String GUARDADO = "Listo. Guardamos tus datos.";

  /** "12 de noviembre de 2026". */
  static final DateTimeFormatter FECHA =
      DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-AR"));

  private final DirectorioDeAdministradores directorio;
  private final AdministradorActual actual;
  private final Pantallas pantallas;
  private final CuentasDelMenu cuentas;

  AdminPerfilController(
      DirectorioDeAdministradores directorio,
      AdministradorActual actual,
      Pantallas pantallas,
      CuentasDelMenu cuentas) {
    this.directorio = directorio;
    this.actual = actual;
    this.pantallas = pantallas;
    this.cuentas = cuentas;
  }

  @GetMapping("/" + PANTALLA)
  String perfil(Model model, HttpServletRequest pedido) {
    String cuenta = cuentaDe(pedido);
    String vista = pantallas.mostrar(PANTALLA, model);
    cargar(cuenta, model, pedido)
        .ifPresent(propio -> valores(model, propio.nombre(), propio.apellido()));
    return vista;
  }

  @PostMapping("/" + PANTALLA)
  String guardar(
      @RequestParam(required = false) String nombre,
      @RequestParam(required = false) String apellido,
      Model model,
      HttpServletRequest pedido,
      RedirectAttributes atributos) {
    String cuenta = cuentaDe(pedido);
    Map<String, String> errores = DatosDeAdministrador.perfil(nombre, apellido);
    if (errores.isEmpty()) {
      try {
        directorio.cambiarNombre(
            cuenta, DatosDeAdministrador.limpio(nombre), DatosDeAdministrador.limpio(apellido));
        // El menú y el saludo vuelven a pedir el nombre: ya es el nuevo (R11-44).
        cuentas.olvidar(pedido);
        return Envios.listo(PANTALLA, GUARDADO, atributos);
      } catch (RestClientException noRespondio) {
        model.addAttribute("errorGuardar", true);
        model.addAttribute("codigo", codigo(pedido));
      }
    }
    String vista = pantallas.mostrar(PANTALLA, model);
    cargar(cuenta, model, pedido);
    model.addAttribute("errores", errores);
    valores(model, nombre, apellido);
    return vista;
  }

  private Optional<Administrador> cargar(String cuenta, Model model, HttpServletRequest pedido) {
    model.addAttribute("errores", Map.of());
    try {
      Optional<Administrador> propio = directorio.cuenta(cuenta);
      model.addAttribute("miPerfil", propio.orElse(null));
      model.addAttribute(
          "creada", propio.map(Administrador::creada).map(FECHA::format).orElse(null));
      model.addAttribute("errorCarga", propio.isEmpty());
      if (propio.isEmpty()) {
        model.addAttribute("codigo", codigo(pedido));
      }
      return propio;
    } catch (RestClientException noRespondio) {
      model.addAttribute("miPerfil", null);
      model.addAttribute("creada", null);
      model.addAttribute("errorCarga", true);
      model.addAttribute("codigo", codigo(pedido));
      return Optional.empty();
    }
  }

  private static void valores(Model model, String nombre, String apellido) {
    model.addAttribute("nombre", nombre);
    model.addAttribute("apellido", apellido);
  }

  private String cuentaDe(HttpServletRequest pedido) {
    return actual
        .cuenta(pedido)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
  }

  /** El código de soporte: el traceId de este pedido (TraceIdFiltro). */
  static String codigo(HttpServletRequest pedido) {
    return pedido.getAttribute(TraceIdFiltro.CLAVE) instanceof String id ? id : null;
  }
}
