package grupo5.cliente.web;

import grupo5.cliente.cuentas.ContrasenaProvisoria;
import grupo5.cliente.puertos.AdministradorActual;
import grupo5.cliente.puertos.DirectorioDeAdministradores;
import grupo5.cliente.puertos.DirectorioDeAdministradores.Administrador;
import grupo5.cliente.puertos.DirectorioDeAdministradores.Alta;
import grupo5.cliente.puertos.DirectorioDeAdministradores.AltaIncompleta;
import grupo5.cliente.puertos.DirectorioDeAdministradores.CorreoEnUso;
import grupo5.cliente.vistas.Tramo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
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
 * Administradores (R11-10, ficha docs/interfaz/flujos/r11-p1-admins.md §4.2 a §4.5): las cuentas de
 * administración de a 10, con el total del servicio; el alta de otra con su contraseña provisoria,
 * que se muestra una sola vez; y otra contraseña provisoria para la cuenta que todavía no entró.
 * Todo bajo {@code /admin-*}: solo la administración llega acá (SeguridadConfig).
 *
 * <p>La contraseña viaja a su pantalla en un atributo flash (se lee una vez), no se guarda ni se
 * loguea, y esa pantalla va con {@code Cache-Control: no-store}. Recargarla o volver con el
 * navegador lleva a la lista con el aviso de que ya se mostró. En la maqueta, la pantalla de la
 * contraseña sin envío muestra una de ejemplo.
 *
 * <p>Un alta con error de datos o del servicio: con HTMX vuelve solo el formulario de la ventana,
 * que sigue abierta (ficha §4.3); sin JavaScript, la página aparte {@code
 * admin-administradores-nuevo}.
 */
@Controller
public class AdminAdministradoresController {

  static final String PANTALLA = "admin-administradores";
  static final String NUEVO = PANTALLA + "-nuevo";
  static final String ALTA = PANTALLA + "-alta";
  static final String PROVISORIA = "provisoria";

  /** El id del formulario del alta, el mismo en la ventana y en la página aparte. */
  static final String FORMULARIO = "alta-formulario";

  static final String YA_MOSTRADA =
      "La contraseña provisoria se mostró una sola vez. Si no la anotaste, generá otra desde la"
          + " cuenta.";

  /** En la sesión: el aviso de "ya puede entrar" de la última contraseña mostrada. */
  static final String AVISO_DEL_ALTA = "administradores.avisoDelAlta";

  private static final String ERRORES = "errores";
  private static final String CODIGO = "codigo";

  /**
   * Una cuenta en la lista: {@code nombre} es null si no tiene nombre cargado; {@code creada}, la
   * fecha en texto ("12 de noviembre de 2026"); {@code propia}, si es la de la sesión.
   */
  record Fila(
      String id, String nombre, String correo, String creada, boolean entro, boolean propia) {

    static Fila de(Administrador cuenta, String propia) {
      return new Fila(
          cuenta.id(),
          cuenta.nombreCompleto(),
          cuenta.correo(),
          cuenta.creada() == null ? null : AdminPerfilController.FECHA.format(cuenta.creada()),
          cuenta.entro(),
          cuenta.id().equals(propia));
    }
  }

  /** Lo que muestra la pantalla de la contraseña: una sola vez. */
  record Provisoria(String nombre, String correo, String contrasena, boolean otra) {}

  private final DirectorioDeAdministradores directorio;
  private final AdministradorActual actual;
  private final Pantallas pantallas;
  private final boolean maqueta;

  AdminAdministradoresController(
      DirectorioDeAdministradores directorio,
      AdministradorActual actual,
      Pantallas pantallas,
      Environment entorno) {
    this.directorio = directorio;
    this.actual = actual;
    this.pantallas = pantallas;
    this.maqueta = entorno.acceptsProfiles(Profiles.of("maqueta"));
  }

  @GetMapping("/" + PANTALLA)
  String lista(Model model, HttpServletRequest pedido) {
    String propia = cuentaDe(pedido);
    String vista = pantallas.mostrar(PANTALLA, model);
    model.addAttribute("propia", propia);
    avisoDelAlta(model, pedido);
    try {
      long total = directorio.total();
      if (total <= 0) {
        // Quien la mira es admin: cero cuentas es una falla del servicio, no un vacío.
        throw new RestClientException("El servicio no devolvió cuentas de administración");
      }
      int paginas = Tramo.paginas(total, DirectorioDeAdministradores.POR_PAGINA);
      int pagina = Math.clamp(Paginadores.pedida(pedido), 1, paginas);
      List<Administrador> cuentas = directorio.pagina(pagina);
      Tramo<Administrador> tramo = new Tramo<>(cuentas, total, pagina, paginas);
      model.addAttribute("total", total);
      model.addAttribute("filas", cuentas.stream().map(c -> Fila.de(c, propia)).toList());
      model.addAttribute("paginador", Paginadores.de(pedido, "Páginas de administradores", tramo));
    } catch (RestClientException noRespondio) {
      model.addAttribute("filas", null);
      model.addAttribute(CODIGO, AdminPerfilController.codigo(pedido));
    }
    return vista;
  }

  @GetMapping("/" + NUEVO)
  String nuevo(Model model, HttpServletRequest pedido) {
    cuentaDe(pedido);
    String vista = pantallas.mostrar(NUEVO, model);
    model.addAttribute(ERRORES, Map.of());
    return vista;
  }

  @PostMapping("/" + PANTALLA)
  String alta(
      @RequestParam(required = false) String nombre,
      @RequestParam(required = false) String apellido,
      @RequestParam(required = false) String correo,
      Model model,
      HttpServletRequest pedido,
      HttpServletResponse respuesta,
      RedirectAttributes atributos) {
    String propia = cuentaDe(pedido);
    Map<String, String> errores = DatosDeAdministrador.alta(nombre, apellido, correo);
    if (errores.isEmpty()) {
      Alta datos =
          new Alta(
              DatosDeAdministrador.limpio(nombre),
              DatosDeAdministrador.limpio(apellido),
              DatosDeAdministrador.limpio(correo));
      String contrasena = ContrasenaProvisoria.nueva();
      try {
        directorio.alta(datos, contrasena, propia);
        atributos.addFlashAttribute(
            PROVISORIA,
            new Provisoria(
                datos.nombre() + " " + datos.apellido(), datos.correo(), contrasena, false));
        return "redirect:/" + ALTA;
      } catch (CorreoEnUso repetido) {
        errores = Map.of(DatosDeAdministrador.CORREO, DatosDeAdministrador.CORREO_EN_USO);
      } catch (AltaIncompleta | RestClientException noSeCreo) {
        model.addAttribute("errorAlta", true);
        model.addAttribute(CODIGO, AdminPerfilController.codigo(pedido));
      }
    }
    String vista = pantallas.mostrar(NUEVO, model);
    model.addAttribute(ERRORES, errores);
    model.addAttribute("nombre", nombre);
    model.addAttribute("apellido", apellido);
    model.addAttribute("correo", correo);
    if (pedido.getHeader("HX-Request") != null) {
      // Con HTMX la ventana sigue abierta: vuelve solo su formulario, con los errores junto al
      // campo
      // y lo escrito, y reemplaza al que está adentro de la ventana (no a la lista).
      respuesta.setHeader("HX-Retarget", "#" + FORMULARIO);
      respuesta.setHeader("HX-Reselect", "#" + FORMULARIO);
      respuesta.setHeader("HX-Reswap", "outerHTML");
      return PANTALLA + " :: ventana";
    }
    return vista;
  }

  @GetMapping("/" + ALTA)
  String contrasena(
      Model model,
      HttpServletRequest pedido,
      HttpServletResponse respuesta,
      RedirectAttributes atributos) {
    cuentaDe(pedido);
    HttpSession sesion = pedido.getSession();
    if (!(model.getAttribute(PROVISORIA) instanceof Provisoria provisoria)) {
      if (!maqueta) {
        sesion.removeAttribute(AVISO_DEL_ALTA);
        return Envios.listo(PANTALLA, YA_MOSTRADA, atributos);
      }
      model.addAttribute(
          PROVISORIA,
          new Provisoria("Bruno Díaz", "bruno.diaz@ejemplo.org", "K7pQ-m4Rt-x9Wa-2Lse", false));
    } else {
      sesion.setAttribute(
          AVISO_DEL_ALTA,
          provisoria.nombre() + " ya puede entrar con su correo y la contraseña provisoria.");
    }
    respuesta.setHeader("Cache-Control", "no-store");
    return pantallas.mostrar(ALTA, model);
  }

  @PostMapping("/" + PANTALLA + "-contrasena")
  String otraContrasena(
      @RequestParam String cuenta, HttpServletRequest pedido, RedirectAttributes atributos) {
    cuentaDe(pedido);
    try {
      Administrador elegida =
          directorio
              .cuenta(cuenta)
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
      String nombre =
          elegida.nombreCompleto() == null ? elegida.correo() : elegida.nombreCompleto();
      String contrasena = ContrasenaProvisoria.nueva();
      if (!directorio.otraContrasena(cuenta, contrasena)) {
        return Envios.listo(
            PANTALLA,
            nombre
                + " ya eligió su contraseña: no hace falta otra. Si la olvidó, escribinos desde"
                + " Contacto.",
            atributos);
      }
      atributos.addFlashAttribute(
          PROVISORIA, new Provisoria(nombre, elegida.correo(), contrasena, true));
      return "redirect:/" + ALTA;
    } catch (RestClientException noRespondio) {
      String codigo = AdminPerfilController.codigo(pedido);
      return Envios.listo(
          PANTALLA,
          "No pudimos generar otra contraseña. La anterior sigue sirviendo. Probá de nuevo en"
              + " unos minutos."
              + (codigo == null ? "" : " Código: " + codigo + "."),
          atributos);
    }
  }

  /** El aviso de "ya puede entrar", una sola vez, al volver de la contraseña. */
  private static void avisoDelAlta(Model model, HttpServletRequest pedido) {
    HttpSession sesion = pedido.getSession(false);
    if (sesion != null && sesion.getAttribute(AVISO_DEL_ALTA) instanceof String aviso) {
      sesion.removeAttribute(AVISO_DEL_ALTA);
      if (!model.containsAttribute(Envios.AVISO)) {
        model.addAttribute(Envios.AVISO, aviso);
      }
    }
  }

  private String cuentaDe(HttpServletRequest pedido) {
    return actual
        .cuenta(pedido)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
  }
}
