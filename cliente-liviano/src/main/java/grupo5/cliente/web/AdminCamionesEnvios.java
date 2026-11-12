package grupo5.cliente.web;

import grupo5.cliente.puertos.AdminActividadPuerto;
import grupo5.cliente.puertos.AdminActividadPuerto.AccionDelSitio;
import grupo5.cliente.puertos.AdminOperacionPuerto;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Los envíos de Camiones fuera de la maqueta: agregar, deshabilitar y volver a habilitar. Guardan
 * en Logística y vuelven a la lista, con la ficha del camión y el aviso de la ficha del flujo. En
 * la maqueta los atiende {@code SitioController}, sin guardar. Cada cambio que sale bien se anota
 * en el registro de actividad (R11-26); si la anotación falla, el aviso no cambia.
 */
@Controller
@Profile("!maqueta")
class AdminCamionesEnvios {

  private static final String QUEDA = " Queda disponible para las próximas rutas.";

  private final AdminOperacionPuerto operacion;
  private final AdminActividadPuerto actividad;

  AdminCamionesEnvios(AdminOperacionPuerto operacion, AdminActividadPuerto actividad) {
    this.operacion = operacion;
    this.actividad = actividad;
  }

  @PostMapping("/admin-camiones-agregado")
  String agregar(
      @RequestParam String patente,
      @RequestParam float capacidadKG,
      @RequestParam float capacidadVolumen,
      @RequestParam float altura,
      RedirectAttributes atributos) {
    String clave = operacion.agregarCamion(patente.trim(), capacidadKG, capacidadVolumen, altura);
    actividad.anotar(AccionDelSitio.CAMION_AGREGADO, clave, patente.trim());
    return Envios.listo(
        destino(clave), "Agregaste el camión " + patente.trim() + "." + QUEDA, atributos);
  }

  @PostMapping("/admin-camiones-deshabilitado")
  String deshabilitar(
      @RequestParam String camion,
      @RequestParam(required = false) String motivo,
      RedirectAttributes atributos) {
    String patente = operacion.cambiarCamion(camion, false, motivo);
    actividad.anotar(AccionDelSitio.CAMION_DESHABILITADO, camion, patente);
    return Envios.listo(
        destino(camion),
        "Deshabilitaste el camión "
            + patente
            + ". No se le van a asignar rutas hasta que lo vuelvas a habilitar.",
        atributos);
  }

  @PostMapping("/admin-camiones-habilitado")
  String habilitar(@RequestParam String camion, RedirectAttributes atributos) {
    String patente = operacion.cambiarCamion(camion, true, null);
    actividad.anotar(AccionDelSitio.CAMION_HABILITADO, camion, patente);
    return Envios.listo(
        destino(camion), "Volviste a habilitar el camión " + patente + "." + QUEDA, atributos);
  }

  /** La clave ya pasó por la API (un UUID válido): vuelve a la lista con la ficha del camión. */
  private static String destino(String camion) {
    return "admin-camiones?camion=" + camion;
  }
}
