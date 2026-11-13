package grupo5.cliente.web;

import grupo5.cliente.puertos.AdminOperacionPuerto;
import grupo5.cliente.vistas.ConsultaAsignaciones;
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
 * misma organización o la propuesta cambió) vuelve con lo que pasó. El nombre de la organización
 * viaja oculto en el formulario (R11-24: no se pide la lista para buscarlo) y se vuelve a la
 * pestaña desde la que se decidió. En la maqueta los atiende {@code SitioController}, sin guardar.
 */
@Controller
@Profile("!maqueta")
class AdminAsignacionesEnvios {

  private static final String LISTA = "admin-asignaciones";
  private static final int LARGO = 200;
  private static final int LARGO_DEL_MOTIVO = 500;
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
  String aprobar(
      @RequestParam String propuesta,
      @RequestParam(required = false) String organizacion,
      @RequestParam(required = false) String pestana,
      RedirectAttributes atributos) {
    try {
      int aConflictos = operacion.decidirPropuesta(propuesta, true, null);
      String aviso =
          "Aprobaste la propuesta"
              + para(organizacion)
              + ". Las donaciones quedaron asignadas y salen del depósito cuando se arme la ruta.";
      if (aConflictos == 1) {
        aviso += " 1 propuesta que pedía la misma donación pasó a Conflictos.";
      } else if (aConflictos > 1) {
        aviso +=
            " " + aConflictos + " propuestas que pedían la misma donación pasaron a Conflictos.";
      }
      return Envios.listo(volver(pestana), aviso, atributos);
    } catch (HttpClientErrorException.Conflict rechazo) {
      return Envios.listo(
          volver(pestana), porQue(rechazo.getResponseBodyAsString(), organizacion), atributos);
    }
  }

  @PostMapping("/admin-asignaciones-descartada")
  String descartar(
      @RequestParam String propuesta,
      @RequestParam(required = false) String motivo,
      @RequestParam(required = false) String organizacion,
      @RequestParam(required = false) String pestana,
      RedirectAttributes atributos) {
    // R11-35: el motivo se guarda; Donaciones acepta hasta 500 caracteres (el campo ya lo topea)
    if (motivo != null && motivo.strip().length() > LARGO_DEL_MOTIVO) {
      return Envios.listo(
          volver(pestana),
          "El motivo puede tener hasta 500 caracteres. Acortalo y volvé a descartar.",
          atributos);
    }
    operacion.decidirPropuesta(propuesta, false, motivo);
    boolean conMotivo = motivo != null && !motivo.isBlank();
    return Envios.listo(
        volver(pestana),
        "Descartaste la propuesta"
            + para(organizacion)
            + (conMotivo ? " y guardamos el motivo" : "")
            + ". Las donaciones siguen en el depósito para otra asignación.",
        atributos);
  }

  /** La pestaña desde la que se decidió (un valor que no se entiende es Propuestas). */
  private static String volver(String pestana) {
    String entendida = ConsultaAsignaciones.de(pestana, null, null, null, null, 1).pestana();
    return ConsultaAsignaciones.PROPUESTAS.equals(entendida)
        ? LISTA
        : LISTA + "?pestana=" + entendida;
  }

  /** El nombre llega en el formulario; uno vacío o demasiado largo no se muestra. */
  private static String para(String organizacion) {
    String nombre = nombre(organizacion);
    return nombre == null ? "" : " para " + nombre;
  }

  private static String nombre(String organizacion) {
    return organizacion == null || organizacion.isBlank() || organizacion.length() > LARGO
        ? null
        : organizacion.strip();
  }

  /**
   * El código del catálogo de errores (docs/arquitectura/catalogo-errores.md) dice por qué no se
   * pudo aprobar: ERR-EST-521 es NECESIDAD_INACTIVA y ERR-EST-520, DONACION_A_SI_MISMO.
   */
  private static String porQue(String cuerpo, String organizacion) {
    if (cuerpo.contains("ERR-EST-521")) {
      String nombre = nombre(organizacion);
      return "No se pudo aprobar: la necesidad"
          + (nombre == null ? "" : " de " + nombre)
          + " ya no está activa (la organización la dio de baja o ya se cumplió)."
          + SIN_ASIGNAR;
    }
    if (cuerpo.contains("ERR-EST-520")) {
      return "No se pudo aprobar: una de las donaciones es de la misma organización que la pidió."
          + SIN_ASIGNAR;
    }
    return "Esta propuesta cambió mientras la mirabas. Volvimos a cargar la lista.";
  }
}
