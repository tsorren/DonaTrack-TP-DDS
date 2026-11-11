package grupo5.cliente.web;

import grupo5.cliente.puertos.AdminOperacionPuerto;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Los envíos de Asignar donaciones fuera de la maqueta: correr la asignación, aprobar y descartar
 * una propuesta. Guardan en Donaciones y vuelven a la lista con el aviso de la ficha del flujo. Una
 * aprobación que Donaciones rechaza (409: la necesidad ya no está activa, la donación es de la
 * misma entidad o la propuesta cambió) vuelve con lo que pasó. En la maqueta los atiende {@code
 * SitioController}, sin guardar.
 */
@Controller
@Profile("!maqueta")
class AdminAsignacionesEnvios {

  private static final String LISTA = "admin-asignaciones";
  private static final String SIN_ASIGNAR = " No se asignó nada; podés descartar la propuesta.";

  private final AdminOperacionPuerto operacion;

  AdminAsignacionesEnvios(AdminOperacionPuerto operacion) {
    this.operacion = operacion;
  }

  @PostMapping("/admin-asignaciones-corrida")
  String correr(RedirectAttributes atributos) {
    int nuevas = operacion.correrAsignacion();
    String aviso;
    if (nuevas == 0) {
      aviso = "La asignación no encontró donaciones para ninguna necesidad nueva.";
    } else {
      aviso =
          nuevas == 1
              ? "La asignación generó 1 propuesta nueva."
              : "La asignación generó " + nuevas + " propuestas nuevas.";
    }
    return Envios.listo(LISTA, aviso, atributos);
  }

  @PostMapping("/admin-asignaciones-aprobada")
  String aprobar(@RequestParam String propuesta, RedirectAttributes atributos) {
    try {
      String entidad = operacion.decidirPropuesta(propuesta, true, null);
      return Envios.listo(
          LISTA,
          "Aprobaste la propuesta"
              + para(entidad)
              + ". Las donaciones quedaron asignadas y salen del depósito cuando se arme la ruta.",
          atributos);
    } catch (HttpClientErrorException.Conflict rechazo) {
      return Envios.listo(LISTA, porQue(rechazo.getResponseBodyAsString()), atributos);
    }
  }

  @PostMapping("/admin-asignaciones-descartada")
  String descartar(
      @RequestParam String propuesta,
      @RequestParam(required = false) String motivo,
      RedirectAttributes atributos) {
    String entidad = operacion.decidirPropuesta(propuesta, false, motivo);
    return Envios.listo(
        LISTA,
        "Descartaste la propuesta"
            + para(entidad)
            + ". Las donaciones siguen en el depósito para otra asignación.",
        atributos);
  }

  private static String para(String entidad) {
    return entidad == null ? "" : " para " + entidad;
  }

  /**
   * El código del catálogo de errores (docs/arquitectura/catalogo-errores.md) dice por qué no se
   * pudo aprobar: ERR-EST-521 es NECESIDAD_INACTIVA y ERR-EST-520, DONACION_A_SI_MISMO.
   */
  private static String porQue(String cuerpo) {
    if (cuerpo.contains("ERR-EST-521")) {
      return "No se pudo aprobar: la necesidad ya no está activa (la entidad la dio de baja o ya se"
          + " cumplió)."
          + SIN_ASIGNAR;
    }
    if (cuerpo.contains("ERR-EST-520")) {
      return "No se pudo aprobar: una de las donaciones es de la misma entidad que la pidió."
          + SIN_ASIGNAR;
    }
    return "Esta propuesta cambió mientras la mirabas. Volvimos a cargar la lista.";
  }
}
