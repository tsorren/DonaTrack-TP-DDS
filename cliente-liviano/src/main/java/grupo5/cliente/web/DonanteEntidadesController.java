package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.DonanteEntidadesPuerto;
import grupo5.cliente.puertos.DonanteEntidadesPuerto.Consulta;
import grupo5.cliente.vistas.DonanteEntidadesVista;
import grupo5.cliente.vistas.Listado;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Entidades del donante (RF-06) con la página que pide la dirección ({@code ?buscar=}, antes {@code
 * ?nombre=}, que sigue valiendo; {@code ?categoria=}, {@code ?ya=si} y {@code ?pagina=}; una página
 * mal escrita es la 1), con la barra del componente listado ({@code listado}), en la versión del
 * tipo de la cuenta. La búsqueda y los filtros los resuelve el puerto sobre todas las entidades
 * (R11-12). El atributo es {@code misEntidades}; {@code paginador}, el componente paginas (null con
 * una sola página). {@code depositoDeEjemplo} es true solo en la maqueta: con datos reales todavía
 * no hay de dónde sacar el depósito (R11-09) y "Cómo llega tu donación" va sin dirección. Sin
 * cuenta, manda a ingresar.
 */
@Controller
public class DonanteEntidadesController {

  private final DonanteEntidadesPuerto entidades;
  private final CuentaActual cuentas;
  private final Pantallas pantallas;
  private final boolean maqueta;

  DonanteEntidadesController(
      DonanteEntidadesPuerto entidades,
      CuentaActual cuentas,
      Pantallas pantallas,
      Environment entorno) {
    this.entidades = entidades;
    this.cuentas = cuentas;
    this.pantallas = pantallas;
    this.maqueta = entorno.acceptsProfiles(Profiles.of("maqueta"));
  }

  @GetMapping("/donante-entidades")
  String entidades(
      @RequestParam(required = false) String buscar,
      @RequestParam(required = false) String nombre,
      @RequestParam(required = false) String categoria,
      @RequestParam(required = false) String ya,
      @RequestParam(required = false) String pagina,
      Model model,
      HttpServletRequest pedido) {
    Optional<UUID> donante = cuentas.donante(pedido);
    if (donante.isEmpty()) {
      return "redirect:/publico-ingresar";
    }
    String vista = pantallas.mostrarDonante("donante-entidades", donante.get(), model, pedido);
    int numero = pagina != null && pagina.matches("[1-9]\\d{0,3}") ? Integer.parseInt(pagina) : 1;
    model.addAttribute("depositoDeEjemplo", maqueta);
    // R11-B04: la búsqueda se llama buscar en todas las listas; nombre sigue valiendo
    Consulta consulta =
        new Consulta(buscar != null ? buscar : nombre, categoria, "si".equals(ya), numero);
    DonanteEntidadesVista misEntidades = entidades.entidades(donante.get(), consulta);
    model.addAttribute("misEntidades", misEntidades);
    model.addAttribute("listado", listado(consulta, misEntidades));
    model.addAttribute(
        "paginador", Paginadores.de(pedido, "Páginas de organizaciones", misEntidades.tramo()));
    return vista;
  }

  /**
   * La barra (R11-B04): la búsqueda por nombre, la categoría y "Ya doné". La organización no tiene
   * fecha, estado, quién ni orden propios: esos campos no van. Una categoría que no está queda en
   * "Todas las categorías".
   */
  private static Listado listado(Consulta consulta, DonanteEntidadesVista vista) {
    List<Listado.Opcion> categorias =
        vista.opcionesDeCategoria().stream()
            .map(c -> new Listado.Opcion(c.valor(), c.texto()))
            .toList();
    String categoria =
        categorias.stream().anyMatch(c -> c.valor().equals(consulta.categoria()))
            ? consulta.categoria()
            : "";
    return Listado.en("entidades", "/donante-entidades", "Buscar organizaciones")
        .buscar("Buscar por nombre", consulta.nombre(), "Por ejemplo: Comedor")
        .categoria(categoria, categorias)
        .casilla("ya", "si", "Ya doné", consulta.soloDonadas())
        .sacar("/donante-entidades")
        .listo();
  }
}
