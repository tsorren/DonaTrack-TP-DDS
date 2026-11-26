package grupo5.cliente.web;

import grupo5.cliente.puertos.AdminOperacionPuerto;
import grupo5.cliente.vistas.AdminDonacionesVista;
import grupo5.cliente.vistas.ConsultaDonaciones;
import grupo5.cliente.vistas.Listado;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.time.LocalDate;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * Donaciones del admin (RF-15 y RF-16), de a una página (R11-01), y el detalle de una, con los
 * datos del puerto; la lista se llama {@code adminDonaciones} ({@code viaje-datos.html} ya usa
 * {@code donaciones}) y el detalle, {@code donacion}. La lista y el detalle son la misma hoja de la
 * plantilla (ADM1-08: la tabla del arquetipo A03, donde cada fila lleva a su detalle; la bandeja
 * con ficha de R11-34 salió). {@code -detalle-vencida} es la pantalla de resultado de la maqueta:
 * fuera de ella no existe.
 */
@Controller
public class AdminDonacionesController {

  private static final String DETALLE = "admin-donaciones-detalle";

  private final AdminOperacionPuerto operacion;
  private final Pantallas pantallas;
  private final boolean maqueta;

  AdminDonacionesController(
      AdminOperacionPuerto operacion, Pantallas pantallas, Environment entorno) {
    this.operacion = operacion;
    this.pantallas = pantallas;
    this.maqueta = entorno.acceptsProfiles(Profiles.of("maqueta"));
  }

  @GetMapping("/admin-donaciones")
  String donaciones(
      @RequestParam(required = false) String estado,
      @RequestParam(required = false) String vence,
      @RequestParam(required = false) String orden,
      @RequestParam(required = false) String buscar,
      @RequestParam(required = false) String categoria,
      @RequestParam(required = false) String desde,
      @RequestParam(required = false) String hasta,
      @RequestParam(required = false) String quien,
      @RequestParam(required = false) String donante,
      @RequestParam(required = false) String donacion,
      Model model,
      HttpServletRequest pedido) {
    if (donacion != null && !donacion.isBlank()) {
      // Un enlace a una donación (como «La cubría» de Decididas o la ficha de Donantes) muestra
      // su detalle ahí mismo, sin redirigir: el detalle resuelve la que no existe
      return detalle(DETALLE, donacion, model);
    }
    String vista = pantallas.mostrar("admin-donaciones", model);
    LocalDate[] rango = Listado.rango(desde, hasta);
    ConsultaDonaciones consulta =
        new ConsultaDonaciones(
            estado,
            "pronto".equals(vence),
            orden,
            Paginadores.pedida(pedido),
            buscar,
            categoria,
            rango[0],
            rango[1],
            operacion.donacionesPorQuien() ? quien : "",
            donante,
            "");
    AdminDonacionesVista datos = operacion.donaciones(consulta, "");
    model.addAttribute("adminDonaciones", datos);
    model.addAttribute("listado", listado(datos, operacion.donacionesPorQuien()));
    model.addAttribute(
        "paginador", Paginadores.de(pedido, "Páginas de donaciones", datos.donaciones()));
    return vista;
  }

  /**
   * La barra (R11-B04): la búsqueda por lo que se donó, la fecha de registro, estado, categoría,
   * quien donó (si la fuente lo filtra por nombre), "Vence" (si la fuente lo resuelve) y el orden
   * que da la fuente. El donante que llega desde su ficha se conserva oculto, también al sacar los
   * filtros.
   */
  private static Listado listado(AdminDonacionesVista datos, boolean conQuien) {
    ConsultaDonaciones c = datos.consulta();
    Listado.Armado barra =
        Listado.en("donaciones", "/admin-donaciones", "Buscar y filtrar las donaciones")
            .oculto("donante", c.donante())
            .buscar("Buscar por lo que se donó", c.buscar(), "Por ejemplo: leche")
            .fecha("Registrada", c.desde(), c.hasta())
            .estado(c.estado(), opciones(AdminDonacionesVista.estados()))
            .categoria(c.categoria(), opciones(datos.categoriasDelFiltro()));
    if (conQuien) {
      barra.quien("Donante", c.quien(), "Por ejemplo: Ana");
    }
    if (datos.conVence()) {
      barra.otra(
          new Listado.Eleccion(
              "vence",
              "Vence",
              c.pronto() ? "pronto" : "",
              opciones(AdminDonacionesVista.vencimientos())));
    }
    return barra
        .orden(c.orden(), opciones(datos.ordenes()))
        .sacar(
            c.donante().isEmpty()
                ? "/admin-donaciones"
                : "/admin-donaciones?donante=" + c.donante())
        .listo();
  }

  private static List<Listado.Opcion> opciones(List<AdminDonacionesVista.Opcion> opciones) {
    return opciones.stream().map(o -> new Listado.Opcion(o.valor(), o.texto())).toList();
  }

  /** El detalle, pedido por su clave ({@code ?donacion=}): ya no se busca en la lista entera. */
  @GetMapping({"/" + DETALLE, "/" + DETALLE + "-vencida"})
  String detalle(
      @RequestParam(required = false) String donacion, Model model, HttpServletRequest pedido) {
    return detalle(pedido.getRequestURI().substring(1), donacion, model);
  }

  private String detalle(String pantalla, String donacion, Model model) {
    boolean vencida = pantalla.endsWith("-vencida");
    if (vencida && !maqueta) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe esa pantalla");
    }
    String vista = pantallas.mostrar(pantalla, model);
    AdminDonacionesVista.Donacion elegida = operacion.donacion(donacion, vencida ? "vencida" : "");
    if (elegida == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontramos esa donación");
    }
    model.addAttribute("donacion", elegida);
    return vista;
  }

  /**
   * Registrar donación (RF-15): los donantes que coinciden con {@code ?buscar=} (hasta 10) y el
   * elegido con {@code ?donante=}. {@code -registrar-error} y {@code -registrada} son pantallas de
   * la maqueta. {@code -registrada} es Donaciones con el aviso breve (DA-06, ADM1-11): la misma
   * lista, armada como si se pidiera {@code /admin-donaciones}, para que el paginador siga ahí; el
   * envío real ya vuelve a Donaciones con el aviso ({@code AdminDonacionesEnvios}).
   */
  @GetMapping({
    "/admin-donaciones-registrar",
    "/admin-donaciones-registrar-error",
    "/admin-donaciones-registrada"
  })
  String registrar(
      @RequestParam(required = false) String buscar,
      @RequestParam(required = false) String donante,
      Model model,
      HttpServletRequest pedido) {
    String pantalla = pedido.getRequestURI().substring(1);
    if (!"admin-donaciones-registrar".equals(pantalla) && !maqueta) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe esa pantalla");
    }
    if ("admin-donaciones-registrada".equals(pantalla)) {
      HttpServletRequest comoDonaciones =
          new HttpServletRequestWrapper(pedido) {
            @Override
            public String getRequestURI() {
              return "/admin-donaciones";
            }
          };
      donaciones(null, null, null, null, null, null, null, null, null, null, model, comoDonaciones);
      return pantallas.mostrar(pantalla, model);
    }
    String vista = pantallas.mostrar(pantalla, model);
    model.addAttribute("registro", operacion.registro(buscar, donante));
    // R11-B04: la búsqueda del donante es la barra de las listas, solo con Buscar
    model.addAttribute(
        "listado",
        Listado.en("donantes", "/admin-donaciones-registrar", "Quién dona")
            .buscar("Buscar por nombre o documento", buscar, "Por ejemplo: comedor")
            .sacar("/admin-donaciones-registrar")
            .listo());
    return vista;
  }
}
