package grupo5.cliente.web;

import grupo5.cliente.puertos.AdminOperacionPuerto;
import grupo5.cliente.vistas.ConsultaAsignaciones;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Los envíos de Asignar donaciones fuera de la maqueta: correr la asignación, aprobar y descartar
 * propuestas. Guardan en Donaciones y vuelven a la lista con el aviso de la ficha del flujo. Una
 * aprobación que Donaciones rechaza (409: la necesidad ya no está activa, la donación es de la
 * misma organización o la propuesta cambió) vuelve con lo que pasó. El nombre de la organización
 * viaja oculto en el formulario de una sola propuesta (R11-24: no se pide la lista para buscarlo) y
 * se vuelve a la pestaña desde la que se decidió; con {@code pestana=inicio}, a Por decidir ({@code
 * /admin-inicio}). La tabla con selección manda varias {@code propuesta} (DA-26): se deciden de a
 * una contra el puerto, en orden; si una falla, se para y el aviso dice cuántas se decidieron y por
 * qué no la siguiente. Con una sola, el aviso de siempre. En la maqueta los atiende {@code
 * SitioController}, sin guardar.
 */
@Controller
@Profile("!maqueta")
class AdminAsignacionesEnvios {

  private static final String LISTA = "admin-asignaciones";
  private static final String INICIO = "inicio";
  private static final int LARGO = 200;
  private static final int LARGO_DEL_MOTIVO = 500;
  private static final String SIN_ASIGNAR = " No se asignó nada; podés descartar la propuesta.";
  private static final String ASIGNADAS =
      ". Las donaciones quedaron asignadas y salen del depósito cuando se arme la ruta.";
  private static final String EN_EL_DEPOSITO =
      ". Las donaciones siguen en el depósito para otra asignación.";
  private static final String INACTIVA = "ERR-EST-521";
  private static final String PROPIA = "ERR-EST-520";

  private final AdminOperacionPuerto operacion;

  AdminAsignacionesEnvios(AdminOperacionPuerto operacion) {
    this.operacion = operacion;
  }

  @PostMapping("/admin-asignaciones-corrida")
  String correr(@RequestParam(required = false) String pestana, RedirectAttributes atributos) {
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
    return Envios.listo(volver(pestana), aviso, atributos);
  }

  @PostMapping("/admin-asignaciones-aprobada")
  String aprobar(
      @RequestParam(required = false) List<String> propuesta,
      @RequestParam(required = false) String organizacion,
      @RequestParam(required = false) String pestana,
      RedirectAttributes atributos) {
    List<String> elegidas = elegidas(propuesta);
    if (elegidas.isEmpty()) {
      return Envios.listo(volver(pestana), "Elegí al menos una propuesta para aprobar.", atributos);
    }
    int aConflictos = 0;
    int hechas = 0;
    try {
      for (String una : elegidas) {
        aConflictos += operacion.decidirPropuesta(una, true, null);
        hechas++;
      }
    } catch (HttpClientErrorException.Conflict rechazo) {
      String cuerpo = rechazo.getResponseBodyAsString();
      String aviso =
          elegidas.size() == 1
              ? porQue(cuerpo, organizacion)
              : aMedias(
                  new Lote("Aprobaste ", "aprobó", "aprobar"), hechas, elegidas.size(), cuerpo);
      return Envios.listo(volver(pestana), aviso, atributos);
    }
    String aviso =
        (elegidas.size() == 1
                ? "Aprobaste la propuesta" + para(organizacion)
                : "Aprobaste las " + elegidas.size() + " propuestas")
            + ASIGNADAS;
    if (aConflictos == 1) {
      aviso += " 1 propuesta que pedía la misma donación pasó a Conflictos.";
    } else if (aConflictos > 1) {
      aviso += " " + aConflictos + " propuestas que pedían la misma donación pasaron a Conflictos.";
    }
    return Envios.listo(volver(pestana), aviso, atributos);
  }

  @PostMapping("/admin-asignaciones-descartada")
  String descartar(
      @RequestParam(required = false) List<String> propuesta,
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
    List<String> elegidas = elegidas(propuesta);
    if (elegidas.isEmpty()) {
      return Envios.listo(
          volver(pestana), "Elegí al menos una propuesta para descartar.", atributos);
    }
    int hechas = 0;
    try {
      for (String una : elegidas) {
        operacion.decidirPropuesta(una, false, motivo);
        hechas++;
      }
    } catch (HttpClientErrorException.Conflict rechazo) {
      if (elegidas.size() == 1) {
        throw rechazo;
      }
      return Envios.listo(
          volver(pestana),
          aMedias(
              new Lote("Descartaste ", "descartó", "descartar"),
              hechas,
              elegidas.size(),
              rechazo.getResponseBodyAsString()),
          atributos);
    }
    boolean conMotivo = motivo != null && !motivo.isBlank();
    return Envios.listo(
        volver(pestana),
        (elegidas.size() == 1
                ? "Descartaste la propuesta" + para(organizacion)
                : "Descartaste las " + elegidas.size() + " propuestas")
            + (conMotivo ? " y guardamos el motivo" : "")
            + EN_EL_DEPOSITO,
        atributos);
  }

  /** Las propuestas elegidas, sin vacías ni repetidas, en el orden en que llegaron. */
  private static List<String> elegidas(List<String> propuesta) {
    return propuesta == null
        ? List.of()
        : propuesta.stream().filter(p -> p != null && !p.isBlank()).distinct().toList();
  }

  /** Las palabras del aviso de un lote: «Aprobaste», «No se aprobó», «No se pudo aprobar». */
  private record Lote(String hecho, String impersonal, String verbo) {}

  /**
   * El aviso de un lote que se cortó (DA-26): cuántas se decidieron, por qué no la siguiente y que
   * las demás quedaron sin decidir.
   */
  private static String aMedias(Lote lote, int hechas, int total, String cuerpo) {
    String aviso =
        hechas == 0
            ? "No se "
                + lote.impersonal()
                + " ninguna de las "
                + total
                + " propuestas. No se pudo "
                + lote.verbo()
                + " la primera: "
            : lote.hecho()
                + hechas
                + " de las "
                + total
                + " propuestas. No se pudo "
                + lote.verbo()
                + " la siguiente: ";
    aviso += razon(cuerpo) + ".";
    return hechas + 1 < total ? aviso + " Las demás quedaron sin decidir." : aviso;
  }

  /**
   * La pestaña desde la que se decidió: {@code inicio} es Por decidir; un valor que no se entiende
   * es Propuestas.
   */
  private static String volver(String pestana) {
    if (INICIO.equals(pestana)) {
      return "admin-inicio";
    }
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
    if (cuerpo.contains(INACTIVA)) {
      String nombre = nombre(organizacion);
      return "No se pudo aprobar: la necesidad"
          + (nombre == null ? "" : " de " + nombre)
          + " ya no está activa (la organización la dio de baja o ya se cumplió)."
          + SIN_ASIGNAR;
    }
    if (cuerpo.contains(PROPIA)) {
      return "No se pudo aprobar: una de las donaciones es de la misma organización que la pidió."
          + SIN_ASIGNAR;
    }
    return "Esta propuesta cambió mientras la mirabas. Volvimos a cargar la lista.";
  }

  /**
   * El porqué de un rechazo dentro de un lote, sin el nombre: no se sabe de qué organización es.
   */
  private static String razon(String cuerpo) {
    if (cuerpo.contains(INACTIVA)) {
      return "la necesidad ya no está activa (la organización la dio de baja o ya se cumplió)";
    }
    if (cuerpo.contains(PROPIA)) {
      return "una de las donaciones es de la misma organización que la pidió";
    }
    return "cambió mientras la mirabas";
  }
}
