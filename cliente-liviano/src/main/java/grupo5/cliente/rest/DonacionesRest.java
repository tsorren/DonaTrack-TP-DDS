package grupo5.cliente.rest;

import static grupo5.cliente.rest.Comun.ARGENTINA;
import static grupo5.cliente.rest.Comun.ASIGNADA;
import static grupo5.cliente.rest.Comun.DIA;
import static grupo5.cliente.rest.Comun.DIA_Y_ANIO;
import static grupo5.cliente.rest.Comun.DIA_Y_HORA;
import static grupo5.cliente.rest.Comun.ENTREGADA;
import static grupo5.cliente.rest.Comun.FALLIDA;
import static grupo5.cliente.rest.Comun.HORA;
import static grupo5.cliente.rest.Comun.LISTA;
import static grupo5.cliente.rest.Comun.PUERTA;
import static grupo5.cliente.rest.Comun.TRASLADO;
import static grupo5.cliente.rest.Comun.VENCIDA;
import static grupo5.cliente.rest.Comun.cantidad;
import static grupo5.cliente.rest.Comun.categoria;
import static grupo5.cliente.rest.Comun.desde;
import static grupo5.cliente.rest.Comun.icono;
import static grupo5.cliente.rest.Comun.normalizado;

import grupo5.cliente.puertos.DonacionesPuerto;
import grupo5.cliente.puertos.DonacionesPuerto.Consulta;
import grupo5.cliente.rest.DonacionesApi.Cambio;
import grupo5.cliente.rest.DonacionesApi.Categoria;
import grupo5.cliente.vistas.DetalleDonacionVista;
import grupo5.cliente.vistas.DetalleDonacionVista.Noticia;
import grupo5.cliente.vistas.DetalleDonacionVista.Problema;
import grupo5.cliente.vistas.EstadoEntrega;
import grupo5.cliente.vistas.MisDonacionesVista;
import grupo5.cliente.vistas.MisDonacionesVista.Donacion;
import grupo5.cliente.vistas.PasoRecorrido;
import grupo5.cliente.vistas.PasoRecorrido.Avance;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;

/**
 * Mis donaciones y su detalle desde Donaciones: una página de las del donante (filtros, página y
 * total en el servidor), las categorías del filtro y el nombre de cada entidad (de las públicas)
 * para el destino. El número de cada donación es su posición, de la primera a la última, entre las
 * que cumplen los filtros. El detalle busca la donación en la lista del donante (otra no se
 * muestra), arma las etapas y las noticias con su historial y, si se entregó, trae la foto de
 * Logística.
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.donaciones", havingValue = "rest")
class DonacionesRest implements DonacionesPuerto {

  /**
   * Cuántas páginas de entidades públicas se recorren como mucho para ponerles nombre (R11-01): 10
   * de 100, mil entidades activas. Más allá, la donación queda con su destino y sin nombre.
   */
  static final int PAGINAS_DE_ENTIDADES = 10;

  /** Las cinco etapas, en orden, con el estado de Donaciones que las abre. */
  private static final List<String> ETAPAS =
      List.of("ENDEPOSITO", ASIGNADA, LISTA, TRASLADO, ENTREGADA);

  /** Los estados de Donaciones que forman cada estado del filtro de Mis donaciones. */
  private static final Map<EstadoEntrega, List<String>> ESTADOS =
      Map.of(
          EstadoEntrega.PENDIENTE,
          List.of("EN_DEPOSITO", "ASIGNACION_REALIZADA", "LISTA_PARA_ENTREGAR"),
          EstadoEntrega.EN_CAMINO,
          List.of("EN_TRASLADO", "EN_PUERTA"),
          EstadoEntrega.ENTREGADA,
          List.of("ENTREGADA"),
          EstadoEntrega.CON_PROBLEMA,
          List.of("ENTREGA_FALLIDA", "VENCIDA"));

  private static final List<String> NOMBRES_ETAPAS =
      List.of("En depósito", "Asignada", "Lista para entregar", "En camino", "Entregada");

  /** El destino de una donación asignada a una entidad cuyo nombre no se pudo leer. */
  static final String SIN_NOMBRE = "una entidad";

  private final DonacionesApi api;
  private final LogisticaApi logistica;
  private final Clock reloj;

  DonacionesRest(DonacionesApi api, LogisticaApi logistica, Clock reloj) {
    this.api = api;
    this.logistica = logistica;
    this.reloj = reloj;
  }

  @Override
  public MisDonacionesVista misDonaciones(UUID donante) {
    return misDonaciones(donante, Consulta.primera());
  }

  /**
   * Una página, con los filtros resueltos por Donaciones (R11-01): el mes por la fecha de registro
   * (desde y hasta inclusivos), el estado como los estados de Donaciones que lo forman y la
   * categoría solo si es una de las del filtro. El total, de {@code X-Total-Count}. Una página más
   * allá de la última pide la última.
   */
  @Override
  public MisDonacionesVista misDonaciones(UUID donante, Consulta consulta) {
    List<MisDonacionesVista.Categoria> categorias =
        api.categorias().stream()
            .map(c -> new MisDonacionesVista.Categoria(c.id().toString(), c.nombre()))
            .toList();
    boolean conCategoria =
        categorias.stream().anyMatch(c -> c.valor().equals(consulta.categoria()));
    MultiValueMap<String, String> filtros = new LinkedMultiValueMap<>();
    filtros.add("donanteId", donante.toString());
    if (consulta.mes() != null) {
      filtros.add("desde", consulta.mes().atDay(1).toString());
      filtros.add("hasta", consulta.mes().atEndOfMonth().toString());
    }
    if (consulta.estado() != null) {
      ESTADOS.get(consulta.estado()).forEach(e -> filtros.add("estado", e));
    }
    if (conCategoria) {
      filtros.add("categoriaId", consulta.categoria());
    }
    int pagina = consulta.pagina();
    ResponseEntity<List<DonacionesApi.Donacion>> respuesta = pedirPagina(filtros, pagina);
    long total = total(respuesta);
    int paginas = (int) Math.max(1, (total + Consulta.POR_PAGINA - 1) / Consulta.POR_PAGINA);
    if (pagina > paginas) {
      pagina = paginas;
      respuesta = pedirPagina(filtros, pagina);
      total = total(respuesta);
    }
    // Donaciones ya las ordena de la más reciente a la primera; se reordena la página por las
    // dudas.
    List<DonacionesApi.Donacion> lista =
        respuesta.getBody() == null
            ? List.of()
            : respuesta.getBody().stream()
                .sorted(Comparator.comparing(DonacionesApi.Donacion::fechaRegistro).reversed())
                .toList();
    Map<UUID, String> entidades = entidades(lista);
    long primero = total - (long) (pagina - 1) * Consulta.POR_PAGINA;
    List<Donacion> donaciones =
        IntStream.range(0, lista.size())
            .mapToObj(i -> donacion(lista.get(i), primero - i, entidades))
            .toList();
    return new MisDonacionesVista(
        donaciones,
        categorias,
        "hoy, " + LocalDateTime.now(reloj.withZone(ARGENTINA)).format(DIA_Y_HORA),
        total,
        pagina,
        paginas);
  }

  private ResponseEntity<List<DonacionesApi.Donacion>> pedirPagina(
      MultiValueMap<String, String> filtros, int pagina) {
    MultiValueMap<String, String> conPagina = new LinkedMultiValueMap<>(filtros);
    conPagina.set("page", String.valueOf(pagina - 1));
    conPagina.set("size", String.valueOf(Consulta.POR_PAGINA));
    return api.pagina(conPagina);
  }

  /** El total de {@code X-Total-Count}; sin la cabecera, lo que vino en la página. */
  private static long total(ResponseEntity<List<DonacionesApi.Donacion>> respuesta) {
    int enLaPagina = respuesta.getBody() == null ? 0 : respuesta.getBody().size();
    String total = respuesta.getHeaders().getFirst("X-Total-Count");
    try {
      return total == null ? enLaPagina : Long.parseLong(total);
    } catch (NumberFormatException malEscrito) {
      return enLaPagina;
    }
  }

  @Override
  public Optional<DetalleDonacionVista> detalle(UUID donante, String clave) {
    List<DonacionesApi.Donacion> lista = ordenadas(donante);
    int posicion =
        IntStream.range(0, lista.size())
            .filter(i -> lista.get(i).id().toString().equals(clave))
            .findFirst()
            .orElse(-1);
    if (posicion < 0) {
      return Optional.empty();
    }
    DonacionesApi.Donacion d = lista.get(posicion);
    Donacion fila = donacion(d, (long) lista.size() - posicion, entidades(List.of(d)));
    String estado = normalizado(d.estadoActual());
    String entidad = fila.destino() == null ? "la entidad" : fila.destino();
    LocalDateTime llego = ENTREGADA.equals(estado) ? desde(d, estado) : null;
    String foto = llego == null ? null : foto(d.id());
    String pie =
        llego == null
            ? null
            : mayuscula(entidad)
                + " confirmó la entrega el "
                + llego.format(DIA)
                + (foto == null ? "." : " y subió esta foto.");
    return Optional.of(
        new DetalleDonacionVista(
            fila,
            llego == null ? null : llego.format(DIA_Y_ANIO),
            pasos(d, estado),
            problema(d, estado, entidad),
            foto,
            foto == null ? null : "Foto de la entrega que subió " + entidad,
            pie,
            noticias(d, entidad, foto != null),
            d.quienDono() == null ? null : d.quienDono().nombreVisible()));
  }

  private List<DonacionesApi.Donacion> ordenadas(UUID donante) {
    return api.donaciones(donante).stream()
        .sorted(Comparator.comparing(DonacionesApi.Donacion::fechaRegistro).reversed())
        .toList();
  }

  /**
   * El nombre de cada entidad de la lista. El donante no puede leer una entidad por id (solo ADMIN
   * o la entidad) y {@code /api/entidades/publicas} no filtra por id, así que se recorren sus
   * páginas de a {@link DonacionesApi#TOPE} (R11-01) hasta tener todas las de la lista, o hasta
   * {@link #PAGINAS_DE_ENTIDADES} páginas. Una dada de baja, una más allá del tope o con Donaciones
   * sin responder esa lista, queda como {@link #SIN_NOMBRE}: tiene destino, sin nombre.
   */
  private Map<UUID, String> entidades(List<DonacionesApi.Donacion> lista) {
    return nombresDeEntidades(api, lista);
  }

  /** También lo usa el seguimiento y el inicio del donante. */
  static Map<UUID, String> nombresDeEntidades(
      DonacionesApi donaciones, List<DonacionesApi.Donacion> lista) {
    // HashMap y no Map.of(): get(null), el de una donación sin destino, devuelve null.
    Map<UUID, String> entidades = new HashMap<>();
    Set<UUID> buscadas =
        lista.stream()
            .map(DonacionesApi.Donacion::entidadBeneficiariaId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    if (buscadas.isEmpty()) {
      return entidades;
    }
    try {
      leerNombres(donaciones, buscadas, entidades);
    } catch (RestClientException sinLista) {
      entidades.clear();
    }
    // Una que no está entre las públicas (dada de baja) o sin la lista: tiene destino, sin nombre.
    buscadas.forEach(id -> entidades.putIfAbsent(id, SIN_NOMBRE));
    return entidades;
  }

  /** Recorre las páginas de las públicas hasta encontrar las buscadas, la última o el tope. */
  private static void leerNombres(
      DonacionesApi donaciones, Set<UUID> buscadas, Map<UUID, String> entidades) {
    Set<UUID> faltan = new HashSet<>(buscadas);
    for (int pagina = 0; pagina < PAGINAS_DE_ENTIDADES && !faltan.isEmpty(); pagina++) {
      MultiValueMap<String, String> filtros = new LinkedMultiValueMap<>();
      filtros.add("page", String.valueOf(pagina));
      filtros.add("size", String.valueOf(DonacionesApi.TOPE));
      ResponseEntity<List<DonacionesApi.EntidadPublica>> respuesta =
          donaciones.entidadesPublicas(filtros);
      List<DonacionesApi.EntidadPublica> publicas =
          Objects.requireNonNullElse(respuesta.getBody(), List.of());
      for (DonacionesApi.EntidadPublica e : publicas) {
        if (e.id() != null && e.razonSocial() != null && !e.razonSocial().isBlank()) {
          entidades.put(e.id(), e.razonSocial());
          faltan.remove(e.id());
        }
      }
      long total = Paginas.total(respuesta, publicas, pagina + 1, DonacionesApi.TOPE);
      if (publicas.size() < DonacionesApi.TOPE
          || (long) (pagina + 1) * DonacionesApi.TOPE >= total) {
        return;
      }
    }
  }

  /**
   * La primera foto de recepción de la entrega, o null: sin entrega (404), sin foto o con Logística
   * caída. La entrega ya está confirmada en Donaciones; sin la foto, la pantalla dice eso solo.
   */
  private String foto(UUID donacion) {
    try {
      LogisticaApi.Seguimiento seguimiento = logistica.seguimiento(donacion);
      LogisticaApi.Entrega entrega =
          seguimiento == null || seguimiento.idEntrega() == null
              ? null
              : logistica.entrega(seguimiento.idEntrega());
      if (entrega == null) {
        return null;
      }
      String url =
          entrega.fotosRecepcion() == null || entrega.fotosRecepcion().isEmpty()
              ? entrega.fotoRecepcionUrl()
              : entrega.fotosRecepcion().getFirst();
      // Solo direcciones web: la URL viene de afuera y va a un src.
      return url != null && (url.startsWith("https://") || url.startsWith("http://")) ? url : null;
    } catch (RestClientException sinFoto) {
      return null;
    }
  }

  /**
   * Las cinco etapas: las anteriores a la actual, hechas con su fecha; la actual con la suya; las
   * que siguen, pendientes. Una entregada tiene todas hechas; una que no pudo llegar se queda en
   * "En camino" y una vencida, en "En depósito".
   */
  static List<PasoRecorrido> pasos(DonacionesApi.Donacion d, String estado) {
    int actual =
        switch (estado) {
          case FALLIDA, PUERTA -> ETAPAS.indexOf(TRASLADO);
          case VENCIDA -> 0;
          default -> Math.max(0, ETAPAS.indexOf(estado));
        };
    boolean terminada = ENTREGADA.equals(estado);
    return IntStream.range(0, ETAPAS.size())
        .mapToObj(
            i -> {
              String nombre = NOMBRES_ETAPAS.get(i);
              if (i > actual) {
                return new PasoRecorrido(nombre, "Pendiente", Avance.PENDIENTE);
              }
              String dia = desde(d, i == actual ? estado : ETAPAS.get(i)).format(DIA);
              if (i < actual || terminada) {
                return new PasoRecorrido(nombre, dia, Avance.HECHO);
              }
              String detalle =
                  switch (estado) {
                    case FALLIDA -> dia + ": no pudo llegar";
                    case VENCIDA -> dia + ": venció";
                    default -> dia;
                  };
              return new PasoRecorrido(nombre, detalle, Avance.ACTUAL);
            })
        .toList();
  }

  /** El aviso de las que tienen problema: qué pasó, el motivo si lo hay y la última noticia. */
  private static Problema problema(DonacionesApi.Donacion d, String estado, String entidad) {
    String titulo =
        switch (estado) {
          case FALLIDA -> "No pudo llegar a " + entidad + ".";
          case VENCIDA -> "Venció en el depósito.";
          default -> null;
        };
    if (titulo == null) {
      return null;
    }
    String motivo = motivo(d, estado);
    return new Problema(
        titulo,
        (motivo == null ? "" : oracion(motivo) + " ")
            + "Última noticia: "
            + desde(d, estado).format(DIA_Y_HORA)
            + ".");
  }

  /** La justificación del último cambio a ese estado, o null. */
  static String motivo(DonacionesApi.Donacion d, String estado) {
    return d.historial() == null
        ? null
        : d.historial().stream()
            .filter(c -> normalizado(c.estadoNuevo()).equals(estado))
            .max(Comparator.comparing(Cambio::timestamp))
            .map(Cambio::justificacion)
            .filter(j -> !j.isBlank())
            .orElse(null);
  }

  /** Una noticia por cada cambio de estado, de la más nueva a la primera. */
  private List<Noticia> noticias(DonacionesApi.Donacion d, String entidad, boolean conFoto) {
    if (d.historial() == null) {
      return List.of();
    }
    LocalDate hoy = LocalDate.now(reloj.withZone(ARGENTINA));
    return d.historial().stream()
        .sorted(Comparator.comparing(Cambio::timestamp).reversed())
        .map(
            c ->
                new Noticia(
                    cuando(c.timestamp(), hoy),
                    c.timestamp().toString(),
                    textoDeNoticia(c, entidad, conFoto)))
        .filter(n -> n.texto() != null)
        .toList();
  }

  static String cuando(LocalDateTime momento, LocalDate hoy) {
    LocalDate dia = momento.toLocalDate();
    if (dia.equals(hoy)) {
      return "Hoy, " + momento.format(HORA);
    }
    return dia.equals(hoy.minusDays(1))
        ? "Ayer, " + momento.format(HORA)
        : momento.format(DIA_Y_HORA);
  }

  static String textoDeNoticia(Cambio cambio, String entidad, boolean conFoto) {
    String justificacion = cambio.justificacion();
    return switch (normalizado(cambio.estadoNuevo())) {
      case "ENDEPOSITO" -> "Recibimos tu donación en el depósito.";
      case ASIGNADA -> "Asignamos tu donación a una necesidad de " + entidad + ".";
      case LISTA -> "Tu donación quedó lista para entregar.";
      case TRASLADO -> "El camión salió a entregar tu donación a " + entidad + ".";
      case PUERTA -> "El camión llegó a " + entidad + ". Falta que confirme la recepción.";
      case ENTREGADA ->
          mayuscula(entidad)
              + " confirmó que recibió tu donación"
              + (conFoto ? " y sumó una foto." : ".");
      case FALLIDA ->
          "El camión no pudo llegar a "
              + entidad
              + (justificacion == null || justificacion.isBlank()
                  ? "."
                  : ": " + minuscula(conPunto(justificacion)));
      case VENCIDA -> "Tu donación venció en el depósito.";
      default -> null;
    };
  }

  /** Empieza con mayúscula y termina con punto. */
  private static String oracion(String texto) {
    return mayuscula(conPunto(texto));
  }

  private static String mayuscula(String texto) {
    return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
  }

  private static String conPunto(String texto) {
    String limpio = texto.strip();
    return limpio.endsWith(".") ? limpio : limpio + ".";
  }

  private static String minuscula(String texto) {
    return Character.toLowerCase(texto.charAt(0)) + texto.substring(1);
  }

  private static Donacion donacion(
      DonacionesApi.Donacion d, long numero, Map<UUID, String> entidades) {
    Categoria categoria = categoria(d);
    String estado = normalizado(d.estadoActual());
    return new Donacion(
        d.id().toString(),
        numero,
        d.descripcion(),
        cantidad(d.cantidad(), categoria),
        categoria == null ? null : categoria.id().toString(),
        categoria == null ? null : icono(categoria.nombre()),
        categoria == null ? null : categoria.nombre(),
        entidades.get(d.entidadBeneficiariaId()),
        d.fechaRegistro().format(DIA_Y_ANIO),
        estadoEntrega(estado),
        nota(estado, desde(d, estado)));
  }

  static EstadoEntrega estadoEntrega(String estado) {
    return switch (estado) {
      case TRASLADO, PUERTA -> EstadoEntrega.EN_CAMINO;
      case ENTREGADA -> EstadoEntrega.ENTREGADA;
      case FALLIDA, VENCIDA -> EstadoEntrega.CON_PROBLEMA;
      default -> EstadoEntrega.PENDIENTE;
    };
  }

  static String nota(String estado, LocalDateTime desde) {
    return switch (estado) {
      case ASIGNADA -> "Asignada, espera el camión";
      case LISTA -> "Lista para entregar, espera el camión";
      case TRASLADO -> "Salió el " + desde.format(DIA_Y_HORA);
      case PUERTA -> "Llegó a la entidad el " + desde.format(DIA_Y_HORA);
      case ENTREGADA -> "Entrega confirmada el " + desde.format(DIA);
      case FALLIDA -> "No pudo llegar el " + desde.format(DIA);
      case VENCIDA -> "Venció en el depósito el " + desde.format(DIA);
      default -> "En el depósito, espera destino";
    };
  }
}
