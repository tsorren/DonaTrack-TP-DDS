package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.DonanteNotificacionesPuerto;
import grupo5.cliente.vistas.ConsultaBandeja;
import grupo5.cliente.vistas.DonanteNotificacionesVista;
import grupo5.cliente.vistas.DonanteNotificacionesVista.Aviso;
import grupo5.cliente.vistas.DonanteNotificacionesVista.Tema;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * La bandeja de ejemplo de la empresa, la que armaba la plantilla con las cartas de viaje-datos: 17
 * avisos de sus donaciones (id: la clave de la donación y el número de su carta) y dos de
 * necesidades nuevas, cada uno con su título según la ficha (R11-14). "Ahora" es el 15 de octubre
 * de 2026, 16:25; sin leer, las 4 más nuevas. Filtra y pagina en memoria lo que en el sistema
 * resuelve Notificaciones (BandejasDeEjemplo). Marcar no guarda: el formulario lo atiende
 * SitioController.
 */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.donante",
    havingValue = "maqueta",
    matchIfMissing = true)
class DonanteNotificacionesMaqueta implements DonanteNotificacionesPuerto {

  private static final String RECIBIMOS = "Recibimos tu donación en el depósito.";
  private static final String ASIGNAMOS = "Asignamos tu donación a una necesidad de ";
  private static final String COMEDOR = "Comedor Barrio Ejemplo";
  private static final String MERENDERO = "Merendero Ejemplo Sur";
  private static final String ESCUELA = "Escuela Ejemplo N.º 12";
  private static final String CAMINO = "camino";
  private static final String ASIGNADAS = "asignadas";
  private static final String DEPOSITO = "deposito";
  private static final String ENTREGADAS = "entregadas";
  private static final String NECESIDADES = "necesidades";
  private static final String ENTIDADES = "/donante-entidades";

  /** El título de cada categoría de ejemplo (ficha, sección 6). */
  private static final Map<String, String> TITULOS =
      Map.of(
          CAMINO,
          "Donación en camino",
          ASIGNADAS,
          "Donación asignada",
          DEPOSITO,
          "Recibida en el depósito",
          ENTREGADAS,
          "Donación entregada",
          "problemas",
          "La entrega tuvo un problema",
          NECESIDADES,
          "Necesidad nueva");

  /** Las categorías del menú: todas las del rol, tengan o no avisos. */
  private static final List<Tema> TEMAS =
      List.of(
          new Tema("", "Todas las categorías", null),
          new Tema(CAMINO, "Donaciones en camino", "camion"),
          new Tema(ENTREGADAS, "Donaciones entregadas", "tilde"),
          new Tema(ASIGNADAS, "Donaciones asignadas", "asigna"),
          new Tema(DEPOSITO, "En el depósito", "caja"),
          new Tema(NECESIDADES, "Necesidades", "necesidad"),
          new Tema("problemas", "Problemas en la entrega", "problema"),
          new Tema("otras", "Otras novedades", "mision"));

  private static final String AHORA = "2026-10-15T16:25";

  private static final List<Aviso> AVISOS =
      List.of(
          deDonacion(
              "lentejas-0",
              "2026-10-15T14:30",
              "El camión salió a entregar tus lentejas a " + COMEDOR + ".",
              CAMINO,
              false),
          deDonacion("arroz-0", "2026-10-14T17:00", RECIBIMOS, DEPOSITO, false),
          deDonacion("leche-0", "2026-10-14T12:15", ASIGNAMOS + MERENDERO + ".", ASIGNADAS, false),
          deDonacion(
              "lentejas-1",
              "2026-10-14T09:10",
              "Tu donación quedó lista para entregar.",
              CAMINO,
              true),
          deDonacion("lentejas-2", "2026-10-13T11:00", ASIGNAMOS + COMEDOR + ".", ASIGNADAS, true),
          deDonacion("leche-1", "2026-10-13T10:00", RECIBIMOS, DEPOSITO, true),
          deDonacion("lentejas-3", "2026-10-12T10:00", RECIBIMOS, DEPOSITO, true),
          deDonacion(
              "utiles-0",
              "2026-10-11T16:10",
              "El camión no pudo llegar a "
                  + ESCUELA
                  + ": el camino estaba cortado. Sale de nuevo el 17 de octubre.",
              "problemas",
              true),
          deDonacion(
              "utiles-1",
              "2026-10-11T09:00",
              "El camión salió a entregar tus útiles escolares.",
              CAMINO,
              true),
          deDonacion(
              "ropa-0",
              "2026-10-09T18:05",
              MERENDERO + " confirmó que recibió tu ropa de abrigo y sumó una foto.",
              ENTREGADAS,
              true),
          deDonacion(
              "ropa-1",
              "2026-10-09T11:40",
              "El camión salió a entregar tu ropa de abrigo.",
              CAMINO,
              true),
          deDonacion("utiles-2", "2026-10-09T10:20", ASIGNAMOS + ESCUELA + ".", ASIGNADAS, true),
          deDonacion("zapatillas-0", "2026-10-06T15:00", RECIBIMOS, DEPOSITO, true),
          deDonacion(
              "fideos-0",
              "2026-10-05T17:20",
              COMEDOR + " confirmó que recibió tus fideos y sumó una foto.",
              ENTREGADAS,
              true),
          deDonacion(
              "fideos-1",
              "2026-10-05T10:00",
              "El camión salió a entregar tus fideos.",
              CAMINO,
              true),
          deDonacion("ropa-2", "2026-10-03T09:30", ASIGNAMOS + MERENDERO + ".", ASIGNADAS, true),
          deDonacion("fideos-2", "2026-10-02T12:00", ASIGNAMOS + COMEDOR + ".", ASIGNADAS, true),
          deNecesidad(
              "x1",
              "2026-10-15T16:02",
              COMEDOR + " publicó una necesidad nueva: 30 kg de arroz para el 25 de octubre.",
              false),
          deNecesidad(
              "x2",
              "2026-09-28T11:00",
              ESCUELA + " publicó una necesidad de útiles escolares para el inicio de octubre.",
              true));

  private static Aviso deNecesidad(String id, String fecha, String texto, boolean leida) {
    return new Aviso(
        id,
        fecha,
        TITULOS.get(NECESIDADES),
        texto,
        NECESIDADES,
        ENTIDADES,
        "Ver Organizaciones",
        leida);
  }

  /** Un aviso de una donación: lleva a su detalle, con la clave que va antes del guion del id. */
  private static Aviso deDonacion(
      String id, String fecha, String texto, String tema, boolean leida) {
    return new Aviso(
        id,
        fecha,
        TITULOS.get(tema),
        texto,
        tema,
        "/donante-donaciones-detalle?donacion=" + id.substring(0, id.indexOf('-')),
        "Ver la donación",
        leida);
  }

  @Override
  public DonanteNotificacionesVista bandeja(UUID persona, ConsultaBandeja consulta) {
    return new DonanteNotificacionesVista(
        BandejasDeEjemplo.pagina(
            AVISOS,
            consulta,
            Aviso::leida,
            Aviso::tema,
            a -> a.titulo() + " " + a.texto(),
            Aviso::fecha),
        AVISOS.stream().filter(a -> !a.leida()).count(),
        TEMAS,
        AHORA);
  }

  @Override
  public void marcar(UUID persona, List<String> ids, boolean leida) {
    // La maqueta no guarda: el formulario lo atiende SitioController.
  }

  @Override
  public void marcarTodas(UUID persona) {
    // La maqueta no guarda: el formulario lo atiende SitioController.
  }
}
