package grupo5.cliente.web;

import grupo5.cliente.puertos.AvisosSinLeer;
import grupo5.cliente.puertos.CuentasDelMenu;
import grupo5.cliente.puertos.DonantePerfilPuerto;
import grupo5.cliente.puertos.DonantePerfilPuerto.Tipo;
import grupo5.cliente.seguridad.Area;
import grupo5.cliente.vistas.EstadoEntrega;
import grupo5.cliente.vistas.OpcionNavegacion;
import grupo5.cliente.vistas.PasoRecorrido;
import grupo5.cliente.vistas.PasoRecorrido.Avance;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

/**
 * Arma la pantalla {@code templates/<pantalla>.html} con la navegación de su área (el prefijo del
 * nombre: publico, donante, entidad o admin) y la cuenta que le da {@link CuentasDelMenu}. La usan
 * la maqueta y el sitio real. Una sección del menú que todavía no tiene plantilla muestra la página
 * de sin dibujar. El menú del donante no tiene Incentivos, que RF-07 oculta a la persona jurídica.
 */
@Component
public class Pantallas {

  /**
   * Los nombres de pantalla que sirve el sitio; el resto (hojas, imágenes, scripts) es estático.
   */
  public static final String NOMBRE = "(?:publico|donante|entidad|admin)(?:-[a-z]+)+";

  private static final String NO_EXISTE = "No existe esa pantalla";

  /**
   * Pantallas que el layout privado enlaza fuera del menú; si no están dibujadas, muestran la
   * página de sin dibujar.
   */
  private static final Map<String, String> FUERA_DEL_MENU = Map.of("perfil", "Mi perfil");

  private static final String SIN_DIBUJAR = "sin-dibujar";
  private static final String INICIO = "Inicio";
  private static final String ICONO_INICIO = "inicio";
  private static final String ICONO_DONACIONES = "donaciones";
  private static final String ICONO_SEGUIMIENTO = "seguimiento";
  private static final String ICONO_INCENTIVOS = "incentivos";

  /** Una sección del menú: su nombre, el corto de la barra del celular, su ícono y su pantalla. */
  private record Seccion(String texto, String textoCorto, String icono, String pantalla) {

    Seccion(String texto, String icono, String pantalla) {
      this(texto, texto, icono, pantalla);
    }
  }

  /** Las secciones de cada área, en el orden del menú. Notificaciones va aparte, en la cabecera. */
  private static final Map<Area, List<Seccion>> MENUS =
      Map.of(
          Area.DONANTE,
          List.of(
              new Seccion(INICIO, ICONO_INICIO, "donante-inicio"),
              new Seccion("Mis donaciones", "Donaciones", ICONO_DONACIONES, "donante-donaciones"),
              new Seccion("Entidades", "entidades", "donante-entidades"),
              new Seccion("Seguimiento", ICONO_SEGUIMIENTO, "donante-seguimiento")),
          Area.ENTIDAD,
          List.of(
              new Seccion(INICIO, ICONO_INICIO, "entidad-inicio"),
              new Seccion("Necesidades", "cat-alimentos", "entidad-necesidades"),
              new Seccion(
                  "Donaciones recibidas", "Recibidas", ICONO_DONACIONES, "entidad-recibidas"),
              new Seccion("Seguir entregas", "Entregas", ICONO_SEGUIMIENTO, "entidad-seguimiento")),
          Area.ADMIN,
          List.of(
              new Seccion(INICIO, ICONO_INICIO, "admin-inicio"),
              new Seccion("Donantes", ICONO_INCENTIVOS, "admin-donantes"),
              new Seccion("Entidades", "entidades", "admin-entidades"),
              new Seccion("Donaciones", ICONO_DONACIONES, "admin-donaciones"),
              new Seccion("Asignar donaciones", "Asignar", ICONO_SEGUIMIENTO, "admin-asignaciones"),
              new Seccion("Camiones", "camiones", "admin-camiones"),
              new Seccion("Ranking", ICONO_INCENTIVOS, "admin-ranking")));

  /**
   * El menú de la persona que dona: el del donante con Misiones antes de Seguimiento (RF-07; la
   * empresa no la ve).
   */
  private static final List<Seccion> MENU_PERSONA =
      List.of(
          MENUS.get(Area.DONANTE).get(0),
          MENUS.get(Area.DONANTE).get(1),
          MENUS.get(Area.DONANTE).get(2),
          new Seccion("Misiones", ICONO_INCENTIVOS, "donante-misiones"),
          MENUS.get(Area.DONANTE).get(3));

  /** Las pantallas del donante con diseño propio de persona, en templates/persona/. */
  private static final Set<String> PROPIAS_DE_PERSONA =
      Set.of("donante-inicio", "donante-perfil", "donante-perfil-guardado");

  /** En la sesión: el tipo de la cuenta del donante y de qué donante es. */
  private static final String TIPO = "donante.tipo";

  private static final String TIPO_DE_QUIEN = "donante.tipo.de";

  /** El recorrido de la donación en camino; viaje-datos toma de acá los avances de los pasos. */
  private static final List<PasoRecorrido> PASOS =
      List.of(
          new PasoRecorrido("En depósito", "12 de octubre", Avance.HECHO),
          new PasoRecorrido("Asignada", "13 de octubre", Avance.HECHO),
          new PasoRecorrido("Lista para entregar", "14 de octubre", Avance.HECHO),
          new PasoRecorrido("En camino", "Hoy, 14:30", Avance.ACTUAL),
          new PasoRecorrido("Entregada", "Pendiente", Avance.PENDIENTE));

  /** Los nombres de las plantillas dibujadas; la dirección del pedido nunca llega al disco. */
  private final Set<String> dibujadas;

  private final CuentasDelMenu cuentas;
  private final ObjectProvider<AvisosSinLeer> avisos;
  private final ObjectProvider<DonantePerfilPuerto> perfiles;

  /**
   * Sin {@link AvisosSinLeer} (los tests que no lo cargan), la cabecera queda sin número; sin
   * {@link DonantePerfilPuerto}, el donante ve la versión de empresa.
   */
  Pantallas(
      ResourceLoader recursos,
      CuentasDelMenu cuentas,
      ObjectProvider<AvisosSinLeer> avisos,
      ObjectProvider<DonantePerfilPuerto> perfiles) {
    this.dibujadas = plantillas(recursos);
    this.cuentas = cuentas;
    this.avisos = avisos;
    this.perfiles = perfiles;
  }

  /** Si hay una plantilla con ese nombre; se compara contra la lista, no se arma una ruta. */
  public boolean existe(String pantalla) {
    return dibujadas.contains(pantalla);
  }

  private static Set<String> plantillas(ResourceLoader recursos) {
    try {
      return Arrays.stream(
              ResourcePatternUtils.getResourcePatternResolver(recursos)
                  .getResources("classpath*:templates/*.html"))
          .map(Resource::getFilename)
          .filter(Objects::nonNull)
          .map(nombre -> nombre.substring(0, nombre.length() - ".html".length()))
          .collect(Collectors.toUnmodifiableSet());
    } catch (IOException falla) {
      throw new UncheckedIOException("No se pudieron listar las plantillas", falla);
    }
  }

  /** La pantalla con los datos del sistema: su modelo y la plantilla de {@code reales/}. */
  public String mostrarReal(String pantalla, Model model) {
    mostrar(pantalla, model);
    return "reales/" + pantalla;
  }

  /** Completa el modelo de la pantalla y devuelve su vista. */
  public String mostrar(String pantalla, Model model) {
    return armar(pantalla, model, false);
  }

  /**
   * La pantalla del donante en la versión de su tipo de cuenta (docs/interfaz/flujos/
   * r11-p0-persona.md): {@code persona} en el modelo es true salvo en la empresa, y {@code sinTipo}
   * true si Donaciones no dice el tipo. La persona confirmada tiene Misiones en el menú; Inicio y
   * Mi perfil de la persona (también sin tipo) son las plantillas de {@code persona/}. El tipo se
   * pide una vez por sesión y por donante; si el pedido falla, no se guarda y la falla sigue su
   * camino (página de error). Sin el puerto (tests de pantalla sueltos), la versión de empresa.
   */
  public String mostrarDonante(
      String pantalla, UUID donante, Model model, HttpServletRequest pedido) {
    Tipo tipo = tipoDe(donante, pedido);
    String vista = armar(pantalla, model, tipo == Tipo.PERSONA);
    boolean persona = tipo != Tipo.EMPRESA;
    model.addAttribute("persona", persona);
    model.addAttribute("sinTipo", tipo == Tipo.SIN_TIPO);
    return persona && PROPIAS_DE_PERSONA.contains(vista) ? "persona/" + vista : vista;
  }

  private Tipo tipoDe(UUID donante, HttpServletRequest pedido) {
    DonantePerfilPuerto puerto = perfiles == null ? null : perfiles.getIfAvailable();
    if (puerto == null) {
      return Tipo.EMPRESA;
    }
    HttpSession sesion = pedido.getSession();
    if (donante.equals(sesion.getAttribute(TIPO_DE_QUIEN))
        && sesion.getAttribute(TIPO) instanceof Tipo guardado) {
      return guardado;
    }
    Tipo tipo = Objects.requireNonNullElse(puerto.tipo(donante), Tipo.SIN_TIPO);
    sesion.setAttribute(TIPO_DE_QUIEN, donante);
    sesion.setAttribute(TIPO, tipo);
    return tipo;
  }

  private String armar(String pantalla, Model model, boolean conMisiones) {
    if (!pantalla.matches(NOMBRE)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, NO_EXISTE);
    }
    boolean dibujada = existe(pantalla);
    String prefijo = pantalla.substring(0, pantalla.indexOf('-'));
    Optional<Area> area = Area.deRuta(prefijo);
    List<Seccion> menu = conMisiones ? MENU_PERSONA : area.map(MENUS::get).orElse(List.of());
    Seccion actual =
        menu.stream()
            .filter(
                seccion ->
                    pantalla.equals(seccion.pantalla())
                        || pantalla.startsWith(seccion.pantalla() + "-"))
            .findFirst()
            .orElse(null);
    String fueraDelMenu = FUERA_DEL_MENU.get(pantalla.substring(prefijo.length() + 1));
    boolean sinDibujar = area.isPresent() && (actual != null || fueraDelMenu != null);
    if (!dibujada && !sinDibujar) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, NO_EXISTE);
    }
    model.addAttribute("completarPerfil", model.containsAttribute("completarPerfil"));
    model.addAttribute("sesionPendiente", model.containsAttribute("sesionPendiente"));
    model.addAttribute("persona", false);
    model.addAttribute("sinTipo", false);
    model.addAttribute("estados", EstadoEntrega.values());
    model.addAttribute("pasos", PASOS);
    model.addAttribute("seccion", actual == null ? fueraDelMenu : actual.texto());
    model.addAttribute(
        "navegacion",
        menu.stream()
            .map(
                seccion ->
                    new OpcionNavegacion(
                        seccion.texto(),
                        seccion.textoCorto(),
                        seccion.icono(),
                        "/" + seccion.pantalla(),
                        seccion.equals(actual)))
            .toList());
    model.addAttribute("cuenta", cuentas.de(prefijo).orElse(null));
    model.addAttribute("sinLeer", area.isPresent() ? sinLeer(prefijo) : 0L);
    model.addAttribute(
        "perfil",
        area.filter(Area::requierePersona).map(a -> "/" + a.prefijo() + "-perfil").orElse(null));
    return dibujada ? pantalla : SIN_DIBUJAR;
  }

  /** Las sin leer de la cuenta, para la cabecera del layout privado. */
  private long sinLeer(String prefijo) {
    AvisosSinLeer conteo = avisos.getIfAvailable();
    if (conteo == null
        || !(RequestContextHolder.getRequestAttributes()
            instanceof ServletRequestAttributes pedido)) {
      return 0;
    }
    return conteo.sinLeer(prefijo, pedido.getRequest());
  }
}
