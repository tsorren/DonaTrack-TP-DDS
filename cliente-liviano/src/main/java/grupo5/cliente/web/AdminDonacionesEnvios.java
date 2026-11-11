package grupo5.cliente.web;

import grupo5.cliente.puertos.AdminOperacionPuerto;
import grupo5.cliente.puertos.CuentasDelMenu;
import grupo5.cliente.vistas.CuentaVista;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Marcar vencida una donación, fuera de la maqueta: guarda en Donaciones (con el nombre del admin
 * en el historial) y vuelve al detalle con el aviso de la ficha del flujo. Si la donación cambió a
 * la vez (409), vuelve al detalle con su estado de nuevo. En la maqueta lo atiende {@code
 * SitioController}, sin guardar.
 */
@Controller
@Profile("!maqueta")
class AdminDonacionesEnvios {

  private static final DateTimeFormatter CUANDO =
      DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm", Locale.forLanguageTag("es-AR"));
  private static final ZoneId ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

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
}
