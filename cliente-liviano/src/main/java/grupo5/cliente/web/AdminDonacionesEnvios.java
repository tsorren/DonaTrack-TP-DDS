package grupo5.cliente.web;

import grupo5.cliente.puertos.AdminOperacionPuerto;
import grupo5.cliente.puertos.AdminOperacionPuerto.BienNuevo;
import grupo5.cliente.puertos.AdminOperacionPuerto.DonacionNueva;
import grupo5.cliente.puertos.CuentasDelMenu;
import grupo5.cliente.vistas.CuentaVista;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Los envíos de Donaciones fuera de la maqueta. Marcar vencida guarda en Donaciones (con el nombre
 * del admin en el historial) y vuelve al detalle con el aviso de la ficha del flujo; si la donación
 * cambió a la vez (409), vuelve al detalle con su estado de nuevo. Registrar donación guarda la
 * donación con sus bienes y fotos y vuelve a la lista. En la maqueta los atiende {@code
 * SitioController}, sin guardar.
 */
@Controller
@Profile("!maqueta")
class AdminDonacionesEnvios {

  private static final DateTimeFormatter CUANDO =
      DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm", Locale.forLanguageTag("es-AR"));
  private static final ZoneId ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");
  private static final String BIEN = "bienes[";

  private final AdminOperacionPuerto operacion;
  private final CuentasDelMenu cuentas;
  private final Clock reloj;

  AdminDonacionesEnvios(AdminOperacionPuerto operacion, CuentasDelMenu cuentas, Clock reloj) {
    this.operacion = operacion;
    this.cuentas = cuentas;
    this.reloj = reloj;
  }

  @PostMapping("/admin-donaciones-detalle-vencida")
  String marcarVencida(
      @RequestParam String donacion,
      @RequestParam(required = false) String motivo,
      RedirectAttributes atributos) {
    String actor = cuentas.de("admin").map(CuentaVista::nombre).orElse("Administración");
    String aviso;
    try {
      operacion.marcarVencida(donacion, actor, motivo);
      aviso =
          "La marcaste como vencida el "
              + CUANDO.format(ZonedDateTime.now(reloj).withZoneSameInstant(ARGENTINA))
              + ".";
    } catch (HttpClientErrorException.Conflict cambio) {
      aviso =
          "Esta donación cambió mientras la mirabas: no la marcamos como vencida. Volvimos a"
              + " cargar su estado.";
    }
    // La clave ya pasó por la API (un UUID válido).
    return Envios.listo("admin-donaciones-detalle?donacion=" + donacion, aviso, atributos);
  }

  /**
   * Registrar donación: quién dona, los bienes ({@code bienes[i].que}, {@code .cantidad}, {@code
   * .vence}, {@code .estado} y {@code .foto}), el depósito y la fecha de recepción. Vuelve a la
   * lista con el aviso. Una cantidad que no es un número vuelve al formulario con el donante
   * elegido; un donante dado de baja (409), al formulario vacío.
   */
  @PostMapping("/admin-donaciones-registrada")
  String registrar(
      @RequestParam UUID donante,
      @RequestParam String descripcion,
      @RequestParam String deposito,
      @RequestParam LocalDate recepcion,
      HttpServletRequest pedido,
      RedirectAttributes atributos) {
    List<BienNuevo> bienes = new ArrayList<>();
    for (int i = 0; pedido.getParameter(BIEN + i + "].que") != null; i++) {
      String cantidad = pedido.getParameter(BIEN + i + "].cantidad");
      if (cantidad == null || !cantidad.trim().matches("\\d{1,9}")) {
        return Envios.listo(
            "admin-donaciones-registrar?donante=" + donante,
            "La cantidad del bien " + (i + 1) + " tiene que ser un número. Por ejemplo: 20.",
            atributos);
      }
      bienes.add(bien(pedido, i, Integer.parseInt(cantidad.trim())));
    }
    try {
      operacion.registrarDonacion(
          new DonacionNueva(donante.toString(), descripcion.trim(), bienes, deposito, recepcion));
    } catch (HttpClientErrorException.Conflict baja) {
      return Envios.listo(
          "admin-donaciones-registrar",
          "Ese donante está dado de baja: no se le pueden registrar donaciones. Si volvió,"
              + " registralo de nuevo en Donantes.",
          atributos);
    }
    return Envios.listo(
        "admin-donaciones",
        "Registraste la donación: "
            + descripcion.trim()
            + (bienes.size() == 1 ? ", 1 bien" : ", " + bienes.size() + " bienes")
            + ". Quedó en el "
            + deposito
            + ".",
        atributos);
  }

  private static BienNuevo bien(HttpServletRequest pedido, int i, int cantidad) {
    String vence = pedido.getParameter(BIEN + i + "].vence");
    String estado = pedido.getParameter(BIEN + i + "].estado");
    MultipartFile foto =
        pedido instanceof MultipartHttpServletRequest conArchivos
            ? conArchivos.getFile(BIEN + i + "].foto")
            : null;
    boolean conFoto = foto != null && !foto.isEmpty();
    return new BienNuevo(
        pedido.getParameter(BIEN + i + "].que").trim(),
        cantidad,
        vence == null || vence.isBlank() ? null : LocalDate.parse(vence),
        estado == null ? null : estado.toUpperCase(Locale.ROOT),
        conFoto ? foto.getContentType() : null,
        conFoto ? foto : null);
  }
}
