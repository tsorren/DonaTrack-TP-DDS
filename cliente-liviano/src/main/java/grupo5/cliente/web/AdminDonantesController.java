package grupo5.cliente.web;

import grupo5.cliente.puertos.AdminPersonasPuerto;
import grupo5.cliente.puertos.DescargaDeFilas;
import grupo5.cliente.vistas.AdminDonantesVista;
import grupo5.cliente.vistas.AdminDonantesVista.Importacion;
import grupo5.cliente.vistas.ConsultaDonantes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Donantes del admin (RF-15 y RF-20) con los datos del puerto, de a una página (R11-01), y la
 * página de avance de una importación (R11-02). Con {@code ?archivo=} (fuera de la maqueta) la
 * importación sale de Donaciones; la página de avance no carga la lista de donantes.
 *
 * <p>El avance se actualiza solo: con HTMX, el bloque {@code #avance} se vuelve a pedir a la misma
 * dirección con {@code ?etapa=} (la etapa que se ve), y la respuesta dice el cambio de etapa en la
 * región que se lee sola. Si Donaciones no responde a ese pedido, quedan las cifras de antes y un
 * aviso breve. La última importación que subió el admin queda en su sesión ({@link #SESION}), para
 * que Donantes muestre la franja con su avance hasta que abra el resultado.
 *
 * <p>R11-20: si mientras se actualiza sola la importación pasa a {@code ERROR} sin ninguna fila
 * leída, la respuesta lleva {@code HX-Redirect} a la página de error (no solo el bloque); una
 * importación que ya no existe vuelve a Importaciones con su aviso; y las filas con error se bajan
 * como CSV, escritas de a tandas mientras llegan.
 */
@Controller
public class AdminDonantesController {

  /** La etapa que se ve en la página, en el pedido de la actualización sola. */
  static final String ETAPA = "etapa";

  /** La última importación que subió el admin en esta sesión. */
  static final String SESION = "adminDonantesImportacion";

  private static final String DONANTES = "admin-donantes";
  private static final String MODELO = "adminDonantes";
  private static final String IMPORTAR_ERROR = "importar-error";

  /** Lo que se guarda en la sesión: el archivo y el nombre con que se subió. */
  record DeLaSesion(UUID archivo, String nombre) implements Serializable {}

  private final AdminPersonasPuerto personas;
  private final Pantallas pantallas;

  AdminDonantesController(AdminPersonasPuerto personas, Pantallas pantallas) {
    this.personas = personas;
    this.pantallas = pantallas;
  }

  @GetMapping("/" + DONANTES)
  String donantes(
      @RequestParam(required = false) String buscar,
      @RequestParam(required = false) String tipo,
      @RequestParam(required = false) String dona,
      @RequestParam(required = false) String estado,
      @RequestParam(required = false) String orden,
      HttpServletRequest pedido,
      HttpSession sesion,
      Model model) {
    String vista = pantallas.mostrar(DONANTES, model);
    ConsultaDonantes consulta =
        new ConsultaDonantes(buscar, tipo, dona, estado, orden, Paginadores.pedida(pedido));
    lista(personas.donantes(consulta).conImportacion(franja(sesion)), pedido, model);
    return vista;
  }

  @GetMapping("/" + DONANTES + "-{accion:importando|importado|importar-error}")
  String importacion(
      @PathVariable String accion,
      @RequestParam(required = false) UUID archivo,
      @RequestParam(required = false) String nombre,
      @RequestParam(name = ETAPA, required = false) String etapa,
      @RequestParam(required = false) String escenario,
      HttpServletRequest pedido,
      HttpServletResponse respuesta,
      HttpSession sesion,
      RedirectAttributes atributos,
      Model model) {
    boolean sondeo = etapa != null && pedido.getHeader("HX-Request") != null;
    String ejemplo = escenario == null ? accion : accion + ":" + escenario;
    Importacion importacion;
    try {
      importacion = personas.importacion(archivo, nombre, ejemplo, Paginadores.pedida(pedido));
    } catch (HttpClientErrorException.NotFound noExiste) {
      if (!sondeo) {
        return Envios.listo(
            AdminImportacionesController.PANTALLA, "No encontramos esa importación.", atributos);
      }
      respuesta.setHeader("HX-Reswap", "none");
      return DONANTES + " :: no-actualizado";
    } catch (RestClientException noRespondio) {
      if (!sondeo) {
        throw noRespondio;
      }
      // Quedan a la vista las cifras de antes: no se cambia el bloque, solo el aviso breve.
      respuesta.setHeader("HX-Reswap", "none");
      return DONANTES + " :: no-actualizado";
    }
    if (importacion == null && !IMPORTAR_ERROR.equals(accion)) {
      return "redirect:/" + DONANTES;
    }
    String cual = importacion == null ? accion : importacion.accion();
    String vista = pantallas.mostrar(DONANTES + "-" + cual, model);
    if (archivo != null && (importacion == null || !importacion.enCurso())) {
      olvidar(sesion, archivo);
    }
    if (IMPORTAR_ERROR.equals(cual)) {
      if (sondeo) {
        // El bloque #avance no existe en la página de error: va la página entera (DTI-40, ítem 7).
        respuesta.setHeader(
            "HX-Redirect",
            UriComponentsBuilder.fromPath("/" + DONANTES + "-" + IMPORTAR_ERROR)
                .queryParam("archivo", archivo)
                .build()
                .toUriString());
      }
      lista(personas.donantes(ConsultaDonantes.primera()), pedido, model);
    } else {
      model.addAttribute(
          "paginador", Paginadores.de(pedido, "Páginas de filas con error", importacion.filas()));
      model.addAttribute("sondeo", sondeo);
      model.addAttribute("cambioDeEtapa", sondeo && !etapa.equals(importacion.etapa()));
      model.addAttribute(
          "actualizar",
          UriComponentsBuilder.fromPath(pedido.getRequestURI())
              .query(pedido.getQueryString())
              .replaceQueryParam(ETAPA, importacion.etapa())
              .build()
              .toUriString());
    }
    model.addAttribute("importacion", importacion);
    return vista;
  }

  /**
   * Las filas con error como CSV (R11-20): con BOM, separadas por punto y coma, una por renglón,
   * con el mismo texto que la tabla. Se escriben mientras llegan de a tandas; si Donaciones deja de
   * responder a mitad de camino, la última línea lo dice.
   */
  @GetMapping("/" + DONANTES + "-importado/filas-con-error.csv")
  void descargarFilasConError(@RequestParam UUID archivo, HttpServletResponse respuesta)
      throws IOException {
    DescargaDeFilas descarga = personas.filasConError(archivo);
    String nombre =
        descarga.archivo() == null
            ? "filas-con-error.csv"
            : "filas-con-error-" + sinExtension(descarga.archivo()) + ".csv";
    respuesta.setContentType("text/csv;charset=UTF-8");
    respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
    respuesta.setHeader(
        HttpHeaders.CONTENT_DISPOSITION,
        ContentDisposition.attachment()
            .filename(nombre, StandardCharsets.UTF_8)
            .build()
            .toString());
    descarga.contenido().escribir(respuesta.getWriter());
  }

  private static String sinExtension(String nombre) {
    return nombre.toLowerCase(Locale.ROOT).endsWith(".csv")
        ? nombre.substring(0, nombre.length() - 4)
        : nombre;
  }

  private void lista(AdminDonantesVista datos, HttpServletRequest pedido, Model model) {
    model.addAttribute(MODELO, datos);
    model.addAttribute(
        "paginador", Paginadores.de(pedido, "Páginas de donantes", datos.donantes()));
  }

  /**
   * La importación de la sesión para la franja de Donantes: en curso, o terminada y sin abrir. Si
   * ya no existe o Donaciones no responde, no hay franja: la lista se muestra igual.
   */
  private Importacion franja(HttpSession sesion) {
    if (!(sesion.getAttribute(SESION) instanceof DeLaSesion guardada)) {
      return null;
    }
    try {
      Importacion importacion =
          personas.importacion(guardada.archivo(), guardada.nombre(), "importando", 1);
      if (importacion == null || IMPORTAR_ERROR.equals(importacion.accion())) {
        sesion.removeAttribute(SESION);
        return null;
      }
      return importacion;
    } catch (RestClientException noRespondio) {
      return null;
    }
  }

  /** Abrió el resultado: la franja ya no hace falta. */
  private static void olvidar(HttpSession sesion, UUID archivo) {
    if (sesion.getAttribute(SESION) instanceof DeLaSesion guardada
        && guardada.archivo().equals(archivo)) {
      sesion.removeAttribute(SESION);
    }
  }
}
