package grupo5.cliente.web;

import grupo5.cliente.puertos.AdminPersonasPuerto;
import grupo5.cliente.vistas.AdminEntidadesVista;
import grupo5.cliente.vistas.Listado;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

/**
 * Entidades del admin (RF-21 y RF-15) con los datos del puerto, y las pantallas que comparten su
 * hoja después de aprobar, pedir corrección o rechazar (las muestra la maqueta). Reemplaza a la
 * página de solicitudes de antes: las que piden sumarse están en la misma lista. La barra es el
 * componente listado ({@code listado}).
 */
@Controller
public class AdminEntidadesController {

  private static final String ENTIDADES = "admin-entidades";

  private static final List<Listado.Opcion> ESTADOS =
      List.of(
          new Listado.Opcion("activas", "Activas"),
          new Listado.Opcion("pendientes", "Pendientes"),
          new Listado.Opcion("inactivas", "Inactivas"));

  private static final List<Listado.Opcion> NECESIDADES =
      List.of(
          new Listado.Opcion("", "Con y sin necesidades"),
          new Listado.Opcion("con", "Con necesidades vigentes"),
          new Listado.Opcion("sin", "Sin necesidades vigentes"));

  private static final List<Listado.Opcion> TIPOS =
      List.of(
          new Listado.Opcion("", "Todos los tipos"),
          new Listado.Opcion("Comedor", "Comedores"),
          new Listado.Opcion("Merendero", "Merenderos"),
          new Listado.Opcion("Escuela", "Escuelas"),
          new Listado.Opcion("Hogar", "Hogares"),
          new Listado.Opcion("Club", "Clubes"),
          new Listado.Opcion("Biblioteca", "Bibliotecas"),
          new Listado.Opcion("Centro comunitario", "Centros comunitarios"));

  private final AdminPersonasPuerto personas;
  private final Pantallas pantallas;
  private final boolean maqueta;

  AdminEntidadesController(AdminPersonasPuerto personas, Pantallas pantallas, Environment entorno) {
    this.personas = personas;
    this.pantallas = pantallas;
    this.maqueta = entorno.acceptsProfiles(Profiles.of("maqueta"));
  }

  @GetMapping("/" + ENTIDADES)
  String entidades(Model model, HttpServletRequest pedido) {
    return mostrar(ENTIDADES, model, pedido);
  }

  @GetMapping("/" + ENTIDADES + "-{accion:aprobada|correccion|rechazada}")
  String despues(@PathVariable String accion, Model model, HttpServletRequest pedido) {
    return mostrar(ENTIDADES + "-" + accion, model, pedido);
  }

  /**
   * La organización quedó registrada, solo en la maqueta (DA-06, ADM2-13): Organizaciones con el
   * aviso breve del registro. Fuera de ella no existe: el envío real ya vuelve a Organizaciones con
   * el aviso ({@code AdminEntidadesEnvioController}).
   */
  @GetMapping("/" + ENTIDADES + "-registrada")
  String registrada(Model model, HttpServletRequest pedido) {
    if (!maqueta) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe esa pantalla");
    }
    return mostrar(ENTIDADES + "-registrada", model, pedido);
  }

  /**
   * La página pedida ({@code ?pagina=}) con la búsqueda ({@code ?buscar=}) y los filtros ({@code
   * ?estado=}, {@code ?pedido=} y {@code ?tipo=}; {@code ?ver=} y {@code ?pedidos=}, los nombres de
   * antes, siguen valiendo); el paginador provisorio (R11-01) solo si la fuente da la lista de a
   * una página y hay más de una.
   */
  private String mostrar(String pantalla, Model model, HttpServletRequest pedido) {
    String vista = pantallas.mostrar(pantalla, model);
    String pedida = pedido.getParameter("buscar");
    String buscar = pedida == null ? "" : pedida.strip();
    // R11-B04: Estado y Necesidades se llaman estado y pedido en todas las listas
    AdminEntidadesVista.Filtro filtro =
        new AdminEntidadesVista.Filtro(
            alias(pedido, "estado", "ver"),
            alias(pedido, "pedido", "pedidos"),
            pedido.getParameter("tipo"));
    AdminEntidadesVista datos = personas.entidades(buscar, filtro, Paginadores.pedida(pedido));
    boolean paginada = datos != null && datos.enPaginas() != null;
    model.addAttribute("adminEntidades", datos);
    model.addAttribute(
        "listado", listado(filtro, buscar, paginada, pedido.getParameter("entidad")));
    model.addAttribute(
        "paginador",
        Paginadores.de(
            pedido,
            "Páginas de organizaciones",
            datos == null || datos.enPaginas() == null ? null : datos.enPaginas().registradas()));
    return vista;
  }

  /** El parámetro {@code nombre} o, si no vino, el de antes ({@code antes}). */
  private static String alias(HttpServletRequest pedido, String nombre, String antes) {
    String valor = pedido.getParameter(nombre);
    return valor != null ? valor : pedido.getParameter(antes);
  }

  /**
   * La barra (R11-B04): la búsqueda (por nombre o localidad; de a una página, solo por nombre),
   * Estado, Necesidades y Tipo. Las cifras de Estado las pone la plantilla ({@link
   * Listado#conEstados}). La organización elegida se conserva oculta; "Sacar los filtros" la saca
   * con ellos, como antes.
   */
  private static Listado listado(
      AdminEntidadesVista.Filtro filtro, String buscar, boolean paginada, String entidad) {
    return Listado.en("organizaciones", "/" + ENTIDADES, "Filtrar las organizaciones")
        .oculto("entidad", entidad)
        .buscar(
            paginada ? "Buscar por nombre" : "Buscar por nombre o localidad",
            buscar,
            "Por ejemplo: comedor")
        .estado(filtro.ver(), ESTADOS)
        .otra(new Listado.Eleccion("pedido", "Necesidades", filtro.pedidos(), NECESIDADES))
        .otra(new Listado.Eleccion("tipo", "Tipo", filtro.tipo(), TIPOS))
        .sacar("/" + ENTIDADES)
        .listo();
  }
}
