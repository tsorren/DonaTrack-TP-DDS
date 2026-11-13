package grupo5.cliente.rest;

import grupo5.cliente.puertos.AdminActividadPuerto;
import grupo5.cliente.puertos.DirectorioDeAdministradores;
import grupo5.cliente.puertos.DirectorioDeAdministradores.Administrador;
import grupo5.cliente.rest.DonacionesApi.Actividad;
import grupo5.cliente.rest.DonacionesApi.AnotacionActividad;
import grupo5.cliente.rest.NombresDeCuentas.Cuenta;
import grupo5.cliente.rest.NombresDeCuentas.Estado;
import grupo5.cliente.vistas.AdminActividadVista;
import grupo5.cliente.vistas.AdminActividadVista.Fila;
import grupo5.cliente.vistas.ConsultaActividad;
import grupo5.cliente.vistas.Tramo;
import grupo5.cliente.web.TraceIdFiltro;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;

/**
 * El registro de actividad de administración desde Donaciones (R11-26, ficha
 * docs/interfaz/flujos/r11-p2-actividad.md): de a 20, con los grupos de Acción traducidos a sus
 * acciones, las fechas a {@code desde} y {@code hasta}, y {@code yo} a la cuenta de la sesión. La
 * búsqueda por administrador la resuelve el proveedor de ingreso (como mucho 21 cuentas, para saber
 * si son más de 20) y la lista se pide por esas cuentas; sin coincidencias o con más de 20, se
 * avisa y se pide sin ese filtro. Los nombres se leen al mostrar, una vez por cuenta.
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.admin-actividad", havingValue = "rest")
class AdminActividadRest implements AdminActividadPuerto {

  /** De a 20 acciones por página (regla 1 del backlog). */
  static final int POR_PAGINA = 20;

  /** Como mucho 20 cuentas por búsqueda; se piden 21 para saber si son más. */
  static final int MAXIMO_DE_CUENTAS = 20;

  static final String NINGUNA_CUENTA =
      "Ninguna cuenta de administración coincide con «%s». Te mostramos la actividad de todos.";
  static final String DEMASIADAS_CUENTAS =
      "Hay más de 20 cuentas de administración que coinciden con «%s». Escribí más del nombre o el"
          + " correo para encontrarla.";
  static final String SIN_NOMBRES =
      "No pudimos traer los nombres de las cuentas: te mostramos la actividad sin ellos. La"
          + " búsqueda por administrador no está disponible por ahora.";

  static final String CUENTA_SIN_NOMBRE = "Una cuenta de administración";
  static final String CUENTA_QUE_NO_EXISTE = "Una cuenta de administración que ya no existe";

  private static final Logger log = LoggerFactory.getLogger(AdminActividadRest.class);

  private static final Map<String, List<String>> ACCIONES =
      Map.of(
          "propuestas", List.of("PROPUESTA_APROBADA", "PROPUESTA_DESCARTADA", "ASIGNACION_CORRIDA"),
          "donaciones", List.of("DONACION_REGISTRADA", "DONACION_VENCIDA"),
          "donantes", List.of("DONANTE_REGISTRADO", "IMPORTACION_SUBIDA"),
          "organizaciones", List.of("ORGANIZACION_APROBADA", "ORGANIZACION_REGISTRADA"),
          "cuentas", List.of("ADMINISTRADOR_DADO_DE_ALTA", "CONTRASENA_PROVISORIA_NUEVA"),
          "camiones", List.of("CAMION_AGREGADO", "CAMION_DESHABILITADO", "CAMION_HABILITADO"));

  private static final String[] MESES = {
    "ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"
  };
  private static final DateTimeFormatter AL =
      DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm", Comun.ES_AR);
  private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
  private static final ZoneId ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");
  private static final String VER_LA_DONACION = "Ver la donación";
  private static final String VER_LA_IMPORTACION = "Ver la importación";

  private final DonacionesApi donaciones;
  private final DirectorioDeAdministradores directorio;
  private final Clock reloj;

  AdminActividadRest(
      DonacionesApi donaciones, DirectorioDeAdministradores directorio, Clock reloj) {
    this.donaciones = donaciones;
    this.directorio = directorio;
    this.reloj = reloj;
  }

  @Override
  public AdminActividadVista actividad(
      ConsultaActividad consulta, String propia, String escenario) {
    String al = AL.format(LocalDateTime.now(reloj.withZone(ARGENTINA)));
    List<String> avisos = new ArrayList<>(consulta.avisos());
    NombresDeCuentas nombres = new NombresDeCuentas(directorio);
    Set<String> autores = new LinkedHashSet<>();
    if (consulta.soloLoMio() && propia != null) {
      autores.add(propia);
    } else if (consulta.deUnaCuenta()) {
      autores.add(consulta.autor());
    }
    if (!consulta.buscar().isEmpty()) {
      Optional<Set<String>> buscadas = buscar(consulta.buscar(), avisos, nombres);
      if (buscadas.isPresent()) {
        Set<String> encontradas = buscadas.get();
        if (!autores.isEmpty()) {
          encontradas.retainAll(autores);
        }
        if (encontradas.isEmpty()) {
          // La cuenta elegida no es ninguna de las buscadas: no hay nada que pedir.
          return vista(
              al, Tramo.de(List.<Fila>of(), 1, POR_PAGINA), consulta, true, nombres, avisos);
        }
        autores = encontradas;
      }
    }
    List<String> acciones = ACCIONES.getOrDefault(consulta.accion(), List.of());
    boolean filtrada =
        !autores.isEmpty()
            || !acciones.isEmpty()
            || consulta.desde() != null
            || consulta.hasta() != null;
    Set<String> pedidos = autores;
    Tramo<Actividad> pagina =
        Paginas.pedir(
            consulta.pagina(),
            POR_PAGINA,
            numero -> donaciones.actividad(filtros(pedidos, acciones, consulta, numero)));
    List<Fila> filas = pagina.filas().stream().map(a -> fila(a, propia, nombres)).toList();
    return vista(
        al,
        new Tramo<>(filas, pagina.total(), pagina.pagina(), pagina.paginas()),
        consulta,
        filtrada,
        nombres,
        avisos);
  }

  @Override
  public void anotar(AccionDelSitio accion, String objetoId, String detalle) {
    try {
      donaciones.anotarActividad(new AnotacionActividad(accion.name(), objetoId, detalle));
    } catch (RestClientException noSeAnoto) {
      log.warn(
          "No se pudo anotar {} en el registro de actividad (traceId {}): {}",
          accion,
          MDC.get(TraceIdFiltro.CLAVE),
          noSeAnoto.getClass().getSimpleName());
    }
  }

  /**
   * Las cuentas que coinciden con lo buscado; vacío si no se puede filtrar por ellas (ninguna, más
   * de 20 o el proveedor no responde), con su aviso.
   */
  private Optional<Set<String>> buscar(
      String texto, List<String> avisos, NombresDeCuentas nombres) {
    List<Administrador> cuentas;
    try {
      cuentas = directorio.buscar(texto, MAXIMO_DE_CUENTAS + 1);
    } catch (RestClientException noRespondio) {
      nombres.noResponde();
      return Optional.empty();
    }
    if (cuentas.isEmpty()) {
      avisos.add(NINGUNA_CUENTA.formatted(texto));
      return Optional.empty();
    }
    if (cuentas.size() > MAXIMO_DE_CUENTAS) {
      avisos.add(DEMASIADAS_CUENTAS.formatted(texto));
      return Optional.empty();
    }
    Set<String> ids = new LinkedHashSet<>();
    cuentas.forEach(c -> ids.add(c.id()));
    return Optional.of(ids);
  }

  private static AdminActividadVista vista(
      String al,
      Tramo<Fila> filas,
      ConsultaActividad consulta,
      boolean filtrada,
      NombresDeCuentas nombres,
      List<String> avisos) {
    String deQuien = null;
    if (consulta.deUnaCuenta()) {
      Cuenta elegida = nombres.de(consulta.autor());
      deQuien = elegida.estado() == Estado.CONOCIDA ? elegida.nombre() : null;
    }
    if (nombres.sinRespuesta()) {
      avisos.add(SIN_NOMBRES);
    }
    return AdminActividadVista.de(al, filas, consulta, filtrada, deQuien, avisos);
  }

  private static MultiValueMap<String, String> filtros(
      Set<String> autores, List<String> acciones, ConsultaActividad consulta, int pagina) {
    MultiValueMap<String, String> filtros = new LinkedMultiValueMap<>();
    autores.forEach(autor -> filtros.add("autor", autor));
    acciones.forEach(accion -> filtros.add("accion", accion));
    if (consulta.desde() != null) {
      filtros.add("desde", consulta.desde().toString());
    }
    if (consulta.hasta() != null) {
      filtros.add("hasta", consulta.hasta().toString());
    }
    filtros.add("page", String.valueOf(pagina));
    filtros.add("size", String.valueOf(POR_PAGINA));
    return filtros;
  }

  /** A dónde lleva una fila: el texto del enlace, la dirección y lo que oye el lector. */
  private record Enlace(String ver, String direccion, String lector) {}

  private static final Enlace SIN_ENLACE = new Enlace(null, null, null);

  /** Un renglón, con las palabras de la ficha §4.1. */
  static Fila fila(Actividad actividad, String propia, NombresDeCuentas nombres) {
    Cuenta autor = nombres.de(actividad.autor());
    boolean esPropia = actividad.autor() != null && actividad.autor().equals(propia);
    String accion = actividad.accion() == null ? "" : actividad.accion();
    Enlace enlace = enlace(accion, actividad.objetoId(), actividad.detalle());
    LocalDateTime ocurrio = actividad.ocurrioEn();
    return new Fila(
        ocurrio == null ? null : cuando(ocurrio),
        ocurrio == null ? null : ISO.format(ocurrio),
        quien(autor, esPropia),
        autor.estado() == Estado.NO_EXISTE ? null : actividad.autor(),
        que(accion, actividad.objetoId(), actividad.detalle(), nombres),
        enlace.ver(),
        enlace.direccion(),
        enlace.lector());
  }

  private static String quien(Cuenta autor, boolean esPropia) {
    return switch (autor.estado()) {
      case CONOCIDA -> esPropia ? autor.nombre() + " (vos)" : autor.nombre();
      case SIN_RESPUESTA -> esPropia ? "Vos" : CUENTA_SIN_NOMBRE;
      case NO_EXISTE -> CUENTA_QUE_NO_EXISTE;
    };
  }

  /** La frase en pasado de cada acción. */
  private static String que(
      String accion, String objeto, String detalle, NombresDeCuentas nombres) {
    return switch (accion) {
      case "PROPUESTA_APROBADA" ->
          con(detalle, "Aprobó una propuesta.", "Aprobó la propuesta para ", ".");
      case "PROPUESTA_DESCARTADA" ->
          con(detalle, "Descartó una propuesta.", "Descartó la propuesta para ", ".");
      case "ASIGNACION_CORRIDA" -> asignacion(detalle);
      case "DONACION_REGISTRADA" -> "Registró una donación en el depósito.";
      case "DONACION_VENCIDA" -> "Marcó una donación como vencida.";
      case "DONANTE_REGISTRADO" ->
          con(detalle, "Registró a una persona donante.", "Registró a ", " como donante.");
      case "IMPORTACION_SUBIDA" ->
          con(
              detalle,
              "Subió un archivo de donantes para importar.",
              "Subió el archivo ",
              " para importar.");
      case "ORGANIZACION_APROBADA" ->
          con(detalle, "Aprobó la solicitud de una organización.", "Aprobó la solicitud de ", ".");
      case "ORGANIZACION_REGISTRADA" ->
          con(detalle, "Registró una organización.", "Registró a ", ".");
      case "ADMINISTRADOR_DADO_DE_ALTA" -> alta(nombres.de(objeto));
      case "CONTRASENA_PROVISORIA_NUEVA" ->
          "Generó otra contraseña provisoria para " + cuenta(nombres.de(objeto)) + ".";
      case "CAMION_AGREGADO" -> con(detalle, "Agregó un camión.", "Agregó el camión ", ".");
      case "CAMION_DESHABILITADO" ->
          con(detalle, "Deshabilitó un camión.", "Deshabilitó el camión ", ".");
      case "CAMION_HABILITADO" ->
          con(detalle, "Volvió a habilitar un camión.", "Volvió a habilitar el camión ", ".");
      default -> "Hizo una acción de administración.";
    };
  }

  /** Solo la donación vencida y la importación tienen pantalla propia. */
  private static Enlace enlace(String accion, String objeto, String detalle) {
    if (!esUuid(objeto)) {
      return SIN_ENLACE;
    }
    return switch (accion) {
      case "DONACION_VENCIDA" ->
          new Enlace(
              VER_LA_DONACION,
              "/admin-donaciones-detalle?donacion=" + objeto,
              "Ver la donación marcada como vencida");
      case "IMPORTACION_SUBIDA" ->
          new Enlace(
              VER_LA_IMPORTACION,
              "/admin-donantes-importado?archivo=" + objeto,
              con(detalle, VER_LA_IMPORTACION, "Ver la importación de ", ""));
      default -> SIN_ENLACE;
    };
  }

  /** {@code sin} si no hay detalle; si no, el detalle entre {@code antes} y {@code despues}. */
  private static String con(String detalle, String sin, String antes, String despues) {
    return detalle == null ? sin : antes + detalle + despues;
  }

  private static String asignacion(String detalle) {
    if (detalle == null || !detalle.strip().matches("\\d{1,9}")) {
      return "Corrió la asignación.";
    }
    int nuevas = Integer.parseInt(detalle.strip());
    return switch (nuevas) {
      case 0 -> "Corrió la asignación: ninguna propuesta nueva.";
      case 1 -> "Corrió la asignación: 1 propuesta nueva.";
      default ->
          "Corrió la asignación: "
              + NumberFormat.getIntegerInstance(Comun.ES_AR).format(nuevas)
              + " propuestas nuevas.";
    };
  }

  private static String alta(Cuenta cuenta) {
    return switch (cuenta.estado()) {
      case CONOCIDA -> "Dio de alta a " + cuenta.nombre() + " como administrador.";
      case NO_EXISTE -> "Dio de alta una cuenta de administración que ya no existe.";
      case SIN_RESPUESTA -> "Dio de alta una cuenta de administración.";
    };
  }

  private static String cuenta(Cuenta cuenta) {
    return switch (cuenta.estado()) {
      case CONOCIDA -> cuenta.nombre();
      case NO_EXISTE -> "una cuenta de administración que ya no existe";
      case SIN_RESPUESTA -> "una cuenta de administración";
    };
  }

  private static String cuando(LocalDateTime momento) {
    return momento.getDayOfMonth()
        + " "
        + MESES[momento.getMonthValue() - 1]
        + " "
        + momento.getYear()
        + ", "
        + String.format("%02d:%02d", momento.getHour(), momento.getMinute());
  }

  private static boolean esUuid(String valor) {
    if (valor == null) {
      return false;
    }
    try {
      UUID.fromString(valor);
      return true;
    } catch (IllegalArgumentException noEs) {
      return false;
    }
  }
}
