package grupo5.cliente.rest;

import static grupo5.cliente.vistas.AdminAsignacionesVista.MENOS_RECIBIERON;
import static grupo5.cliente.vistas.AdminAsignacionesVista.PARECIDO;

import grupo5.cliente.puertos.DirectorioDeAdministradores;
import grupo5.cliente.puertos.DirectorioDeAdministradores.Administrador;
import grupo5.cliente.vistas.AdminAsignacionesVista;
import grupo5.cliente.vistas.AdminAsignacionesVista.Cubre;
import grupo5.cliente.vistas.AdminAsignacionesVista.Cuentas;
import grupo5.cliente.vistas.AdminAsignacionesVista.Decidida;
import grupo5.cliente.vistas.AdminAsignacionesVista.DonacionEnJuego;
import grupo5.cliente.vistas.AdminAsignacionesVista.Opcion;
import grupo5.cliente.vistas.AdminAsignacionesVista.Propuesta;
import grupo5.cliente.vistas.AdminAsignacionesVista.Sugerencia;
import grupo5.cliente.vistas.ConsultaAsignaciones;
import grupo5.cliente.vistas.ConsultaDecididas;
import grupo5.cliente.vistas.Tramo;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;

/**
 * Asignar donaciones desde Donaciones (R11-24, ficha r11-p1-asignaciones). Cada pestaña hace un
 * número fijo de pedidos, todos acotados: una página de la lista, el resumen (las cifras de las
 * pestañas) y la última ejecución; Propuestas suma las 3 decididas más recientes. Nunca pide la
 * lista entera ni una necesidad o una organización por propuesta: la propuesta ya trae la
 * organización, la unidad, quién la sugirió y su situación. Todas las decididas (R11-35) suma la
 * búsqueda y los filtros resueltos en Donaciones, el nombre de quien decidió (por {@link
 * AutoresDelHistorial}), las opciones de "Decidió" (hasta {@link #CUENTAS_EN_EL_MENU} cuentas del
 * directorio, de a 10) y cuántas decididas no tienen fecha (del resumen).
 */
final class AdminOperacionAsignaciones {

  static final int POR_PAGINA = 10;
  static final int DECIDIDAS_POR_PAGINA = 20;
  static final int EN_DESTINO = 20;
  static final int RECIENTES = 3;
  static final int CUENTAS_EN_EL_MENU = 30;

  private static final String ESTADO = "estado";
  private static final String SITUACION = "situacion";
  private static final String PENDIENTE = "PENDIENTE";
  private static final String APROBADA = "APROBADA";
  private static final String DESCARTADA = "DESCARTADA";
  private static final String SIN_STOCK = "SIN_STOCK";
  private static final String UNA_ORGANIZACION = "Una organización";
  private static final String SIN_DATO =
      "Sin el dato del algoritmo: se generó antes de que lo guardáramos.";

  private final DonacionesApi donaciones;
  private final LocalDateTime ahora;
  private final DirectorioDeAdministradores cuentas;
  private final AutoresDelHistorial autores;

  /** {@code ahora}: la hora de la Argentina, para "hoy, 0:00" y los días en el depósito. */
  AdminOperacionAsignaciones(DonacionesApi donaciones, LocalDateTime ahora) {
    this(donaciones, ahora, null);
  }

  /** {@code cuentas}: el directorio que da el nombre de quien decidió (null: sin nombres). */
  AdminOperacionAsignaciones(
      DonacionesApi donaciones, LocalDateTime ahora, DirectorioDeAdministradores cuentas) {
    this.donaciones = donaciones;
    this.ahora = ahora;
    this.cuentas = cuentas;
    this.autores = AutoresDelHistorial.con(cuentas);
  }

  AdminAsignacionesVista vista(ConsultaAsignaciones consulta) {
    if (ConsultaAsignaciones.DECIDIDAS.equals(consulta.pestana())) {
      ConsultaDecididas busqueda = consulta.decididas();
      Tramo<Decidida> decididas =
          mapear(
              Paginas.pedir(
                  consulta.pagina(),
                  DECIDIDAS_POR_PAGINA,
                  n -> donaciones.propuestas(decididas(n, DECIDIDAS_POR_PAGINA, busqueda))),
              p -> decidida(p, true));
      long sinFecha =
          busqueda.decidida().isEmpty()
              ? 0
              : Optional.ofNullable(donaciones.resumenPropuestas())
                  .map(r -> numero(r.decididasSinFecha()))
                  .orElse(0L);
      return new AdminAsignacionesVista(
          consulta,
          null,
          null,
          null,
          List.of(),
          decididas,
          categorias(),
          null,
          null,
          opcionesDeAutor(busqueda),
          sinFecha);
    }
    Cuentas cuentas = cuentas();
    String ultima = ultima();
    return switch (consulta.pestana()) {
      case ConsultaAsignaciones.CONFLICTOS ->
          new AdminAsignacionesVista(
              consulta,
              cuentas,
              ultima,
              pendientes(consulta, List.of("EN_CONFLICTO", SIN_STOCK)),
              List.of(),
              null,
              List.of(),
              null,
              null);
      case ConsultaAsignaciones.DESTINO -> destino(consulta, cuentas, ultima);
      default ->
          new AdminAsignacionesVista(
              consulta,
              cuentas,
              ultima,
              pendientes(consulta, List.of("LIBRE")),
              recientes(),
              null,
              List.of(),
              null,
              null);
    };
  }

  /**
   * Aprueba o descarta. Al aprobar, cuántas pendientes se quedaron sin stock por esta aprobación:
   * la diferencia de las que ya no alcanzan, antes y después (dos cuentas, sin listas).
   */
  int decidir(UUID propuesta, boolean aprobar, String motivo) {
    long antes = aprobar ? numero(donaciones.resumenPropuestas().sinStock()) : 0;
    donaciones.decidirPropuesta(
        propuesta,
        new DonacionesApi.Decision(
            aprobar ? APROBADA : DESCARTADA,
            motivo == null || motivo.isBlank() ? null : motivo.trim()));
    if (!aprobar) {
      return 0;
    }
    long despues = numero(donaciones.resumenPropuestas().sinStock());
    return (int) Math.max(0, despues - antes);
  }

  // --- Las pestañas ------------------------------------------------------------------------------

  private Tramo<Propuesta> pendientes(ConsultaAsignaciones consulta, List<String> situaciones) {
    return mapear(
        Paginas.pedir(
            consulta.pagina(),
            POR_PAGINA,
            n -> {
              MultiValueMap<String, String> filtros = pagina(n, POR_PAGINA);
              filtros.add(ESTADO, PENDIENTE);
              situaciones.forEach(s -> filtros.add(SITUACION, s));
              String algoritmo = algoritmo(consulta.algoritmo());
              if (algoritmo != null) {
                filtros.add("algoritmo", algoritmo);
              }
              return donaciones.propuestas(filtros);
            }),
        p -> propuesta(p, null));
  }

  private List<Decidida> recientes() {
    List<DonacionesApi.Propuesta> filas =
        Objects.requireNonNullElse(
            donaciones.propuestas(decididas(0, RECIENTES, ConsultaDecididas.ninguna())).getBody(),
            List.of());
    return filas.stream().map(p -> decidida(p, false)).toList();
  }

  private AdminAsignacionesVista destino(
      ConsultaAsignaciones consulta, Cuentas cuentas, String ultima) {
    if (consulta.conDonacion()) {
      UUID donacion = uuid(consulta.donacion());
      Tramo<DonacionesApi.Propuesta> pagina =
          donacion == null
              ? new Tramo<>(List.of(), 0, 1, 1)
              : Paginas.pedir(
                  consulta.pagina(),
                  EN_DESTINO,
                  n -> {
                    MultiValueMap<String, String> filtros = pagina(n, EN_DESTINO);
                    filtros.add(ESTADO, PENDIENTE);
                    filtros.add("donacion", donacion.toString());
                    return donaciones.propuestas(filtros);
                  });
      DonacionEnJuego elegida =
          pagina.filas().stream()
              .flatMap(p -> fragmentos(p).stream())
              .filter(
                  f ->
                      f.donacionIndependiente() != null
                          && donacion.equals(f.donacionIndependiente().id()))
              .findFirst()
              .map(f -> elegida(f, pagina.total()))
              .orElse(null);
      return new AdminAsignacionesVista(
          consulta,
          cuentas,
          ultima,
          mapear(pagina, p -> propuesta(p, donacion)),
          List.of(),
          null,
          List.of(),
          null,
          elegida);
    }
    UUID categoria = uuid(consulta.categoria());
    Tramo<DonacionEnJuego> enJuego =
        mapear(
            Paginas.pedir(
                consulta.pagina(),
                POR_PAGINA,
                n -> {
                  MultiValueMap<String, String> filtros = pagina(n, POR_PAGINA);
                  if (categoria != null) {
                    filtros.add("categoriaId", categoria.toString());
                  }
                  return donaciones.donacionesEnPropuestas(filtros);
                }),
            this::enJuego);
    return new AdminAsignacionesVista(
        consulta, cuentas, ultima, null, List.of(), null, categorias(), enJuego, null);
  }

  private List<Opcion> categorias() {
    return Objects.requireNonNullElse(donaciones.categorias(), List.<DonacionesApi.Categoria>of())
        .stream()
        .filter(c -> c.id() != null && c.nombre() != null)
        .map(c -> new Opcion(c.id().toString(), c.nombre()))
        .toList();
  }

  /**
   * Las opciones de "Decidió": Cualquiera, Vos, cada cuenta de administración por su nombre (si son
   * hasta 30; se piden de a 10) y "Antes de las cuentas". Si el directorio no responde o son más de
   * 30, solo las tres fijas; una cuenta elegida que no está en la lista se agrega con su nombre,
   * para que la pastilla diga lo aplicado.
   */
  private List<Opcion> opcionesDeAutor(ConsultaDecididas busqueda) {
    List<Opcion> opciones = new ArrayList<>();
    opciones.add(new Opcion("", "Cualquiera"));
    opciones.add(new Opcion(ConsultaDecididas.VOS, "Vos"));
    if (cuentas != null) {
      try {
        long total = cuentas.total();
        if (total <= CUENTAS_EN_EL_MENU) {
          int paginas = (int) Math.ceil(total / (double) DirectorioDeAdministradores.POR_PAGINA);
          for (int n = 1; n <= paginas; n++) {
            for (Administrador a : cuentas.pagina(n)) {
              if (!a.id().equals(busqueda.vos())) {
                opciones.add(new Opcion(a.id(), autores.de(a.id())));
              }
            }
          }
        }
      } catch (RestClientException noResponde) {
        // Sin el directorio, quedan las opciones fijas
      }
    }
    String elegida = busqueda.decidio();
    if (!elegida.isEmpty()
        && !ConsultaDecididas.ANTES.equals(elegida)
        && opciones.stream().noneMatch(o -> o.valor().equals(elegida))) {
      opciones.add(new Opcion(elegida, autores.de(elegida)));
    }
    opciones.add(new Opcion(ConsultaDecididas.ANTES, "Antes de las cuentas"));
    return opciones;
  }

  private Cuentas cuentas() {
    DonacionesApi.ResumenPropuestas r = donaciones.resumenPropuestas();
    if (r == null) {
      return new Cuentas(0, 0, 0);
    }
    return new Cuentas(
        numero(r.pendientes()), numero(r.libres()), numero(r.enConflicto()) + numero(r.sinStock()));
  }

  /** "hoy, 0:00 · generó 12 propuestas", o null si nunca corrió. */
  private String ultima() {
    List<DonacionesApi.Ejecucion> ultimas =
        Objects.requireNonNullElse(donaciones.ejecuciones(pagina(0, 1)).getBody(), List.of());
    return ultimas.stream()
        .filter(e -> e.fechaEjecucion() != null)
        .findFirst()
        .map(e -> cuando(e.fechaEjecucion()) + " · " + genero(e))
        .orElse(null);
  }

  // --- Una propuesta -----------------------------------------------------------------------------

  /** {@code enDestino}: la donación elegida en Destino (su franja no lleva "Comparar"). */
  private Propuesta propuesta(DonacionesApi.Propuesta p, UUID enDestino) {
    DonacionesApi.NecesidadResumen necesidad = p.necesidad();
    String unidad = necesidad == null ? null : necesidad.unidad();
    List<DonacionesApi.Fragmento> fragmentos = fragmentos(p);
    int cubierto = fragmentos.stream().mapToInt(f -> numero(f.cantidadNecesaria())).sum();
    int pedido = necesidad == null ? 0 : numero(necesidad.cantidadNecesitada());
    String alcance =
        cubierto >= pedido
            ? "Cubre la necesidad completa."
            : "Cubre "
                + cubierto
                + " de "
                + Comun.cantidad(pedido, enUnidad(unidad))
                + "; el resto sigue pendiente.";
    List<DonacionesApi.Sugerencia> sugerencias =
        p.sugeridaPor() == null ? List.of() : p.sugeridaPor();
    String situacion =
        switch (p.situacion() == null ? "" : p.situacion()) {
          case SIN_STOCK -> "sin-stock";
          case "EN_CONFLICTO" -> "conflicto";
          default -> "libre";
        };
    Optional<DonacionesApi.Fragmento> compite =
        fragmentos.stream().filter(AdminOperacionAsignaciones::compartida).findFirst();
    return new Propuesta(
        p.id().toString(),
        entidad(p),
        pide(necesidad, unidad),
        cuando(p.fechaCreacion()),
        fragmentos.stream().map(f -> fragmento(f, unidad)).collect(Collectors.joining("; ")),
        alcance,
        p.fechaCreacion() != null && p.fechaCreacion().toLocalDate().equals(hoy()),
        sugerencias.stream().map(AdminOperacionAsignaciones::sugerencia).toList(),
        nota(sugerencias),
        situacion,
        franja(p, situacion, fragmentos, compite.orElse(null), unidad),
        compite
            .map(DonacionesApi.Fragmento::donacionIndependiente)
            .map(DonacionesApi.Donacion::id)
            .filter(id -> !id.equals(enDestino))
            .map(UUID::toString)
            .orElse(null));
  }

  /** La frase del choque (ficha, sección 5); null si no choca. */
  private static String franja(
      DonacionesApi.Propuesta p,
      String situacion,
      List<DonacionesApi.Fragmento> fragmentos,
      DonacionesApi.Fragmento compite,
      String unidad) {
    if ("sin-stock".equals(situacion)) {
      return fragmentos.stream()
          .filter(f -> numero(f.cantidadNecesaria()) > numero(f.disponible()))
          .findFirst()
          .map(f -> yaNoAlcanza(f, unidad))
          .orElse("Ya no alcanza: una de las donaciones ya no está en el depósito.");
    }
    if (compite != null) {
      int otras = Math.max(1, numero(compite.otrasPropuestas()));
      return "Compite por "
          + fragmento(compite, unidad)
          + (otras == 1
              ? " con 1 propuesta más: alcanza para una."
              : " con " + otras + " propuestas más: no alcanza para todas.");
    }
    if (numero(p.otrasPendientesMismaNecesidad()) > 0) {
      return "Cubre la misma necesidad que otra propuesta pendiente. Si aprobás las dos, se asigna"
          + " de más.";
    }
    return null;
  }

  private static String yaNoAlcanza(DonacionesApi.Fragmento f, String unidad) {
    DonacionesApi.Donacion d = f.donacionIndependiente();
    String que =
        (d == null ? "" : d.descripcion())
            + " · "
            + Comun.cantidad(numero(f.cantidadNecesaria()), enUnidad(unidad));
    boolean asignada = d != null && Comun.ASIGNADA.equals(Comun.normalizado(d.estadoActual()));
    return "Ya no alcanza: "
        + que
        + (asignada
            ? " quedó asignada a otra organización. Podés descartarla."
            : " ya no está en el depósito. Podés descartarla.");
  }

  /** Lo que pide esta más lo que piden las otras no entra en lo que queda. */
  private static boolean compartida(DonacionesApi.Fragmento f) {
    int pedida = numero(f.cantidadNecesaria());
    return numero(f.pedidaEnOtras()) > 0
        && pedida <= numero(f.disponible())
        && pedida + numero(f.pedidaEnOtras()) > numero(f.disponible());
  }

  private static Sugerencia sugerencia(DonacionesApi.Sugerencia s) {
    return new Sugerencia(
        nombre(s.algoritmo()), numero(s.puesto()) + ".º de " + numero(s.deCuantas()));
  }

  /** La letra chica: falta el dato, o qué algoritmo eligió las donaciones si fueron los dos. */
  private static String nota(List<DonacionesApi.Sugerencia> sugerencias) {
    if (sugerencias.isEmpty()) {
      return SIN_DATO;
    }
    if (sugerencias.size() < 2) {
      return null;
    }
    return sugerencias.stream()
        .filter(s -> Boolean.TRUE.equals(s.eligioLasDonaciones()))
        .findFirst()
        .map(s -> "Las donaciones son las que eligió " + nombre(s.algoritmo()) + ".")
        .orElse(null);
  }

  private static String nombre(String algoritmo) {
    return "PRIORIDAD_SUBATENDIDOS".equals(algoritmo) ? MENOS_RECIBIERON : PARECIDO;
  }

  /** El valor de la dirección, como lo entiende Donaciones; null: todas. */
  private static String algoritmo(String filtro) {
    return switch (filtro) {
      case ConsultaAsignaciones.PARECIDO -> "COMPATIBILIDAD_SEMANTICA";
      case ConsultaAsignaciones.MENOS_RECIBIERON -> "PRIORIDAD_SUBATENDIDOS";
      case ConsultaAsignaciones.LAS_DOS -> "AMBOS";
      default -> null;
    };
  }

  /** {@code conMotivo}: Todas las decididas muestra el motivo; las 3 recientes, no. */
  private Decidida decidida(DonacionesApi.Propuesta p, boolean conMotivo) {
    String unidad = p.necesidad() == null ? null : p.necesidad().unidad();
    boolean aprobada = APROBADA.equals(p.estado());
    List<DonacionesApi.Fragmento> fragmentos = fragmentos(p);
    return new Decidida(
        entidad(p),
        pide(p.necesidad(), unidad),
        aprobada ? "Aprobada" : "Descartada",
        fragmentos.stream()
            .map(DonacionesApi.Fragmento::donacionIndependiente)
            .filter(Objects::nonNull)
            .map(Comun::categoria)
            .filter(Objects::nonNull)
            .map(DonacionesApi.Categoria::nombre)
            .findFirst()
            .orElse(null),
        quien(p, aprobada),
        conMotivo && !aprobada ? motivo(p.motivoDescarte()) : null,
        fragmentos.stream()
            .filter(f -> f.donacionIndependiente() != null)
            .map(f -> cubre(f, unidad))
            .toList());
  }

  /** "Aprobó Ana Pérez el 12 de noviembre de 2026, 10:15.", o la frase sin dato. */
  private String quien(DonacionesApi.Propuesta p, boolean aprobada) {
    String verbo = aprobada ? "Aprobó" : "Descartó";
    String autor = autores.de(p.decididaPor());
    if (p.fechaDecision() == null) {
      return autor.isEmpty()
          ? "Se decidió antes de que guardáramos quién y cuándo."
          : verbo + " " + autor + ".";
    }
    String cuando =
        Comun.DIA_Y_ANIO.format(p.fechaDecision()) + ", " + Comun.HORA.format(p.fechaDecision());
    return autor.isEmpty()
        ? (aprobada ? "Se aprobó" : "Se descartó") + " el " + cuando + "."
        : verbo + " " + autor + " el " + cuando + ".";
  }

  private static String motivo(String motivo) {
    return motivo == null || motivo.isBlank() ? "" : motivo.strip();
  }

  private static Cubre cubre(DonacionesApi.Fragmento f, String unidad) {
    // fragmento() ya agrega " (de <nombre>)" si el donante no lo ocultó
    return new Cubre(f.donacionIndependiente().id().toString(), fragmento(f, unidad));
  }

  // --- Destino -----------------------------------------------------------------------------------

  private DonacionEnJuego enJuego(DonacionesApi.DonacionEnPropuestas e) {
    DonacionesApi.Donacion d = e.donacionIndependiente();
    long propuestas = numero(e.propuestasPendientes());
    boolean alcanza = !Boolean.FALSE.equals(e.alcanza());
    String desde;
    if (d == null || d.fechaRegistro() == null) {
      desde = null;
    } else {
      long dias = ChronoUnit.DAYS.between(d.fechaRegistro().toLocalDate(), hoy());
      if (dias <= 0) {
        desde = "llegó hoy";
      } else {
        desde = dias == 1 ? "1 día en el depósito" : dias + " días en el depósito";
      }
    }
    return new DonacionEnJuego(
        d == null ? "" : d.id().toString(),
        que(d),
        deQuien(d),
        desde,
        propuestas,
        alcanza,
        resumen(propuestas, alcanza));
  }

  private DonacionEnJuego elegida(DonacionesApi.Fragmento f, long propuestas) {
    DonacionesApi.Donacion d = f.donacionIndependiente();
    int pedida = numero(f.cantidadNecesaria()) + numero(f.pedidaEnOtras());
    boolean alcanza = pedida <= numero(f.disponible());
    LocalDateTime registro = d.fechaRegistro();
    return new DonacionEnJuego(
        d.id().toString(),
        que(d),
        deQuien(d),
        registro == null ? null : "en el depósito desde el " + Comun.DIA.format(registro),
        propuestas,
        alcanza,
        resumen(propuestas, alcanza));
  }

  private static String resumen(long propuestas, boolean alcanza) {
    if (propuestas == 1) {
      return "La pide 1 propuesta.";
    }
    return "La piden "
        + propuestas
        + " propuestas y "
        + (alcanza ? "alcanza para todas." : "no alcanza para todas.");
  }

  /** "Arroz · 25 kg", con la unidad de su categoría. */
  private static String que(DonacionesApi.Donacion d) {
    if (d == null) {
      return "";
    }
    return d.descripcion() + " · " + Comun.cantidad(d.cantidad(), Comun.categoria(d));
  }

  private static String deQuien(DonacionesApi.Donacion d) {
    return d == null || d.quienDono() == null ? null : d.quienDono().nombreVisible();
  }

  // --- Ayudas ------------------------------------------------------------------------------------

  private static MultiValueMap<String, String> pagina(int numero, int tamanio) {
    MultiValueMap<String, String> filtros = new LinkedMultiValueMap<>();
    filtros.add("page", String.valueOf(numero));
    filtros.add("size", String.valueOf(tamanio));
    return filtros;
  }

  /** Las decididas, con la búsqueda y los filtros de Todas las decididas (R11-35). */
  private MultiValueMap<String, String> decididas(int numero, int tamanio, ConsultaDecididas b) {
    MultiValueMap<String, String> filtros = pagina(numero, tamanio);
    if (!ConsultaDecididas.DESCARTADAS.equals(b.estado())) {
      filtros.add(ESTADO, APROBADA);
    }
    if (!ConsultaDecididas.APROBADAS.equals(b.estado())) {
      filtros.add(ESTADO, DESCARTADA);
    }
    filtros.add(
        "orden",
        ConsultaDecididas.ANTIGUAS.equals(b.orden()) ? "decision_antigua" : "decision_reciente");
    if (!b.buscar().isEmpty()) {
      filtros.add("q", b.buscar());
    }
    UUID categoria = uuid(b.categoria());
    if (categoria != null) {
      filtros.add("categoriaId", categoria.toString());
    }
    if (b.autor() != null) {
      filtros.add("decididaPor", b.autor());
    }
    LocalDate[] periodo = b.periodo(hoy());
    if (periodo[0] != null) {
      filtros.add("decididaDesde", periodo[0].toString());
      filtros.add("decididaHasta", periodo[1].toString());
    }
    return filtros;
  }

  private static <T, R> Tramo<R> mapear(Tramo<T> tramo, Function<T, R> mapa) {
    return new Tramo<>(
        tramo.filas().stream().map(mapa).toList(), tramo.total(), tramo.pagina(), tramo.paginas());
  }

  private static List<DonacionesApi.Fragmento> fragmentos(DonacionesApi.Propuesta p) {
    return p.fragmentaciones() == null ? List.of() : p.fragmentaciones();
  }

  private static String entidad(DonacionesApi.Propuesta p) {
    return p.necesidad() == null || p.necesidad().entidadNombre() == null
        ? UNA_ORGANIZACION
        : p.necesidad().entidadNombre();
  }

  /** "Arroz · 25 kg (de Ana Ejemplo)"; sin el nombre si el donante lo ocultó. */
  private static String fragmento(DonacionesApi.Fragmento f, String unidad) {
    DonacionesApi.Donacion d = f.donacionIndependiente();
    String nombre = deQuien(d);
    return (d == null ? "" : d.descripcion())
        + " · "
        + Comun.cantidad(numero(f.cantidadNecesaria()), enUnidad(unidad))
        + (nombre == null ? "" : " (de " + nombre + ")");
  }

  /** "Arroz, 25 kg". */
  private static String pide(DonacionesApi.NecesidadResumen necesidad, String unidad) {
    if (necesidad == null) {
      return "una necesidad que ya no está";
    }
    return necesidad.descripcion()
        + ", "
        + Comun.cantidad(numero(necesidad.cantidadNecesitada()), enUnidad(unidad));
  }

  /** "hoy, 18:36" o "15 de octubre". */
  private String cuando(LocalDateTime fecha) {
    if (fecha == null) {
      return "";
    }
    return fecha.toLocalDate().equals(hoy())
        ? "hoy, " + Comun.HORA.format(fecha)
        : Comun.DIA.format(fecha);
  }

  private LocalDate hoy() {
    return ahora.toLocalDate();
  }

  private static String genero(DonacionesApi.Ejecucion ejecucion) {
    int n = numero(ejecucion.cantidadPropuestasGeneradas());
    if (n == 0) {
      return "no generó propuestas";
    }
    return n == 1 ? "generó 1 propuesta" : "generó " + n + " propuestas";
  }

  private static int numero(Integer valor) {
    return valor == null ? 0 : valor;
  }

  private static long numero(Long valor) {
    return valor == null ? 0 : valor;
  }

  /** Comun.cantidad escribe la unidad de una categoría; la de la necesidad es la misma. */
  private static DonacionesApi.Categoria enUnidad(String unidad) {
    return new DonacionesApi.Categoria(null, null, unidad, null, null, null);
  }

  private static UUID uuid(String clave) {
    try {
      return clave == null || clave.isEmpty() ? null : UUID.fromString(clave);
    } catch (IllegalArgumentException noEs) {
      return null;
    }
  }
}
