package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.AdminActividadPuerto;
import grupo5.cliente.vistas.AdminActividadVista;
import grupo5.cliente.vistas.AdminActividadVista.Fila;
import grupo5.cliente.vistas.ConsultaActividad;
import grupo5.cliente.vistas.Tramo;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * El registro de actividad de ejemplo (R11-26, ficha r11-p2-actividad §5): siete acciones de las
 * tres cuentas de ejemplo (Ana Pérez es la propia), con Quién y Acción aplicados sobre ellas y la
 * búsqueda por esos tres nombres. Escenarios: {@code vacia} (nunca se registró nada), {@code
 * sin-resultados} (los filtros no encuentran nada), {@code sin-nombres} (el proveedor de ingreso no
 * responde) y {@code muchas} (57 acciones, de a 20). Anotar no hace nada.
 */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.admin-actividad",
    havingValue = "maqueta",
    matchIfMissing = true)
class AdminActividadDeEjemplo implements AdminActividadPuerto {

  private static final int POR_PAGINA = 20;
  private static final String AL = "12 de noviembre de 2026, 18:34";
  private static final String ANA = AdministradoresMaqueta.PROPIA;
  private static final String BRUNO = "cuenta-bruno";
  private static final String CARLA = "cuenta-carla";
  private static final Map<String, String> NOMBRES =
      Map.of(ANA, "Ana Pérez", BRUNO, "Bruno Díaz", CARLA, "Carla Gómez");
  private static final String SIN_NOMBRE = "Una cuenta de administración";
  private static final String SIN_NOMBRES =
      "No pudimos traer los nombres de las cuentas: te mostramos la actividad sin ellos. La"
          + " búsqueda por administrador no está disponible por ahora.";

  /** Una acción de ejemplo: su cuenta, su grupo de Acción y lo que dice. */
  private record Ejemplo(
      String autor,
      String grupo,
      LocalDateTime cuando,
      String que,
      String accion,
      String enlace,
      String lectorEnlace) {}

  private static final List<Ejemplo> EJEMPLOS =
      List.of(
          new Ejemplo(
              ANA,
              "propuestas",
              LocalDateTime.of(2026, 11, 12, 18, 20),
              "Aprobó la propuesta para Comedor Barrio Ejemplo.",
              null,
              null,
              null),
          new Ejemplo(
              BRUNO,
              "donantes",
              LocalDateTime.of(2026, 11, 12, 17, 2),
              "Subió el archivo donantes-noviembre.csv para importar.",
              "Ver la importación",
              "/admin-donantes-importado?archivo=7d1a2b3c-0000-4000-8000-000000000001",
              "Ver la importación de donantes-noviembre.csv"),
          new Ejemplo(
              BRUNO,
              "donaciones",
              LocalDateTime.of(2026, 11, 12, 16, 45),
              "Marcó una donación como vencida.",
              "Ver la donación",
              "/admin-donaciones-detalle?donacion=7d1a2b3c-0000-4000-8000-000000000002",
              "Ver la donación marcada como vencida"),
          new Ejemplo(
              ANA,
              "cuentas",
              LocalDateTime.of(2026, 11, 12, 11, 10),
              "Dio de alta a Bruno Díaz como administrador.",
              null,
              null,
              null),
          new Ejemplo(
              null,
              "propuestas",
              LocalDateTime.of(2026, 11, 11, 19, 31),
              "Corrió la asignación: 3 propuestas nuevas.",
              null,
              null,
              null),
          new Ejemplo(
              CARLA,
              "organizaciones",
              LocalDateTime.of(2026, 11, 11, 10, 5),
              "Aprobó la solicitud de Huerta Comunitaria Ejemplo.",
              null,
              null,
              null),
          new Ejemplo(
              CARLA,
              "camiones",
              LocalDateTime.of(2026, 11, 10, 9, 40),
              "Deshabilitó el camión AB 123 CD.",
              null,
              null,
              null));

  @Override
  public AdminActividadVista actividad(
      ConsultaActividad consulta, String propia, String escenario) {
    String cual = escenario == null ? "" : escenario;
    boolean sinNombres = "sin-nombres".equals(cual);
    List<String> avisos = new ArrayList<>(consulta.avisos());
    List<Ejemplo> todas = "vacia".equals(cual) ? List.of() : ejemplos("muchas".equals(cual));
    String autor = consulta.soloLoMio() ? ANA : consulta.autor();
    if (!consulta.buscar().isEmpty() && !sinNombres) {
      String buscado = consulta.buscar().toLowerCase(Locale.ROOT);
      List<String> coinciden =
          NOMBRES.entrySet().stream()
              .filter(n -> n.getValue().toLowerCase(Locale.ROOT).contains(buscado))
              .map(Map.Entry::getKey)
              .toList();
      if (coinciden.isEmpty()) {
        avisos.add(
            "Ninguna cuenta de administración coincide con «"
                + consulta.buscar()
                + "». Te mostramos la actividad de todos.");
      } else {
        todas = todas.stream().filter(e -> coinciden.contains(e.autor())).toList();
      }
    }
    if (sinNombres) {
      avisos.add(SIN_NOMBRES);
    }
    if (!autor.isEmpty()) {
      todas = todas.stream().filter(e -> autor.equals(e.autor())).toList();
    }
    if (!consulta.accion().isEmpty()) {
      todas = todas.stream().filter(e -> consulta.accion().equals(e.grupo())).toList();
    }
    if ("sin-resultados".equals(cual)) {
      todas = List.of();
    }
    List<Fila> filas = todas.stream().map(e -> fila(e, sinNombres)).toList();
    boolean filtrada = consulta.filtrada() || "sin-resultados".equals(cual);
    String deQuien = consulta.deUnaCuenta() ? NOMBRES.get(consulta.autor()) : null;
    return AdminActividadVista.de(
        AL, Tramo.de(filas, consulta.pagina(), POR_PAGINA), consulta, filtrada, deQuien, avisos);
  }

  @Override
  public void anotar(AccionDelSitio accion, String objetoId, String detalle) {
    // La maqueta no guarda nada.
  }

  /** Las siete de siempre; con {@code muchas}, repetidas hacia atrás hasta 57. */
  private static List<Ejemplo> ejemplos(boolean muchas) {
    if (!muchas) {
      return EJEMPLOS;
    }
    List<Ejemplo> todas = new ArrayList<>();
    for (int i = 0; todas.size() < 57; i++) {
      Ejemplo base = EJEMPLOS.get(i % EJEMPLOS.size());
      todas.add(
          new Ejemplo(
              base.autor(),
              base.grupo(),
              base.cuando().minusDays((long) i / EJEMPLOS.size() * 2),
              base.que(),
              base.accion(),
              base.enlace(),
              base.lectorEnlace()));
    }
    return todas;
  }

  private static Fila fila(Ejemplo e, boolean sinNombres) {
    String nombre = e.autor() == null ? null : NOMBRES.get(e.autor());
    String quien;
    if (e.autor() == null) {
      quien = "Una cuenta de administración que ya no existe";
    } else if (sinNombres) {
      quien = ANA.equals(e.autor()) ? "Vos" : SIN_NOMBRE;
    } else {
      quien = ANA.equals(e.autor()) ? nombre + " (vos)" : nombre;
    }
    return new Fila(
        cuando(e.cuando()),
        e.cuando().toString(),
        quien,
        e.autor(),
        e.que(),
        e.accion(),
        e.enlace(),
        e.lectorEnlace());
  }

  private static String cuando(LocalDateTime momento) {
    String[] meses = {
      "ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"
    };
    return momento.getDayOfMonth()
        + " "
        + meses[momento.getMonthValue() - 1]
        + " "
        + momento.getYear()
        + ", "
        + String.format("%02d:%02d", momento.getHour(), momento.getMinute());
  }
}
