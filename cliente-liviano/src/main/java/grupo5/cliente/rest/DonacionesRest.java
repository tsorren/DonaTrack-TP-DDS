package grupo5.cliente.rest;

import grupo5.cliente.puertos.DonacionesPuerto;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

/**
 * Mis donaciones y su detalle desde Donaciones: la lista del donante, las categorías del filtro y,
 * por cada entidad distinta, su nombre para el destino. El número de cada donación es su posición,
 * de la primera a la última. El detalle busca la donación en la lista del donante (otra no se
 * muestra), arma las etapas y las noticias con su historial y, si se entregó, trae la foto de
 * Logística.
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.donaciones", havingValue = "rest")
class DonacionesRest implements DonacionesPuerto {

  private static final Locale ES_AR = Locale.forLanguageTag("es-AR");
  private static final ZoneId ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");
  private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("d 'de' MMMM", ES_AR);
  private static final DateTimeFormatter DIA_Y_ANIO =
      DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", ES_AR);
  private static final DateTimeFormatter DIA_Y_HORA =
      DateTimeFormatter.ofPattern("d 'de' MMMM, H:mm", ES_AR);
  private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("H:mm", ES_AR);

  private static final String ENTREGADA = "ENTREGADA";
  private static final String FALLIDA = "ENTREGAFALLIDA";
  private static final String VENCIDA = "VENCIDA";
  private static final String TRASLADO = "ENTRASLADO";
  private static final String ASIGNADA = "ASIGNACIONREALIZADA";
  private static final String LISTA = "LISTAPARAENTREGAR";

  /** Las cinco etapas, en orden, con el estado de Donaciones que las abre. */
  private static final List<String> ETAPAS =
      List.of("ENDEPOSITO", ASIGNADA, LISTA, TRASLADO, ENTREGADA);

  private static final List<String> NOMBRES_ETAPAS =
      List.of("En depósito", "Asignada", "Lista para entregar", "En camino", "Entregada");

  /** El sprite de categorías tiene cuatro íconos; una categoría sin el suyo no muestra ninguno. */
  private static final Map<String, String> ICONOS =
      Map.of(
          "aliment", "cat-alimentos",
          "ropa", "cat-ropa",
          "vestim", "cat-ropa",
          "escolar", "cat-escolares",
          "mueble", "cat-muebles");

  private static final Map<String, String> UNIDADES =
      Map.of(
          "KILOGRAMO", "kg",
          "LITROS", "l",
          "UNIDADES", "unidades",
          "METRO", "m",
          "METRO_CUADRADO", "m²",
          "METRO_CUBICO", "m³");

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
    List<DonacionesApi.Donacion> lista = ordenadas(donante);
    Map<UUID, String> entidades = entidades(lista);
    List<Donacion> donaciones =
        IntStream.range(0, lista.size())
            .mapToObj(i -> donacion(lista.get(i), lista.size() - i, entidades))
            .toList();
    List<MisDonacionesVista.Categoria> categorias =
        api.categorias().stream()
            .map(c -> new MisDonacionesVista.Categoria(c.id().toString(), c.nombre()))
            .toList();
    return new MisDonacionesVista(
        donaciones,
        categorias,
        "hoy, " + LocalDateTime.now(reloj.withZone(ARGENTINA)).format(DIA_Y_HORA));
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
    Donacion fila = donacion(d, lista.size() - posicion, entidades(List.of(d)));
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
            null));
  }

  private List<DonacionesApi.Donacion> ordenadas(UUID donante) {
    return api.donaciones(donante).stream()
        .sorted(Comparator.comparing(DonacionesApi.Donacion::fechaRegistro).reversed())
        .toList();
  }

  /** El nombre de cada entidad distinta; una sin nombre queda en null (por eso no es toMap). */
  private Map<UUID, String> entidades(List<DonacionesApi.Donacion> lista) {
    Map<UUID, String> entidades = new HashMap<>();
    lista.stream()
        .map(DonacionesApi.Donacion::entidadBeneficiariaId)
        .filter(Objects::nonNull)
        .distinct()
        .forEach(id -> entidades.put(id, nombreDeEntidad(id)));
    return entidades;
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
  private static List<PasoRecorrido> pasos(DonacionesApi.Donacion d, String estado) {
    int actual =
        switch (estado) {
          case FALLIDA -> ETAPAS.indexOf(TRASLADO);
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
  private static String motivo(DonacionesApi.Donacion d, String estado) {
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

  private static String cuando(LocalDateTime momento, LocalDate hoy) {
    LocalDate dia = momento.toLocalDate();
    if (dia.equals(hoy)) {
      return "Hoy, " + momento.format(HORA);
    }
    return dia.equals(hoy.minusDays(1))
        ? "Ayer, " + momento.format(HORA)
        : momento.format(DIA_Y_HORA);
  }

  private static String textoDeNoticia(Cambio cambio, String entidad, boolean conFoto) {
    String justificacion = cambio.justificacion();
    return switch (normalizado(cambio.estadoNuevo())) {
      case "ENDEPOSITO" -> "Recibimos tu donación en el depósito.";
      case ASIGNADA -> "Asignamos tu donación a una necesidad de " + entidad + ".";
      case LISTA -> "Tu donación quedó lista para entregar.";
      case TRASLADO -> "El camión salió a entregar tu donación a " + entidad + ".";
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
    Categoria categoria =
        d.items() == null
                || d.items().isEmpty()
                || d.items().getFirst() == null
                || d.items().getFirst().bien() == null
            ? null
            : d.items().getFirst().bien().categoria();
    if (categoria != null && (categoria.id() == null || categoria.nombre() == null)) {
      categoria = null;
    }
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

  /** "EnDeposito" y "EN_DEPOSITO" (el enum del contrato) quedan iguales: "ENDEPOSITO". */
  private static String normalizado(String estado) {
    return estado == null ? "" : estado.replace("_", "").toUpperCase(Locale.ROOT);
  }

  private static EstadoEntrega estadoEntrega(String estado) {
    return switch (estado) {
      case TRASLADO -> EstadoEntrega.EN_CAMINO;
      case ENTREGADA -> EstadoEntrega.ENTREGADA;
      case FALLIDA, VENCIDA -> EstadoEntrega.CON_PROBLEMA;
      default -> EstadoEntrega.PENDIENTE;
    };
  }

  /** Cuándo entró al estado en el que está: su último cambio a ese estado, o el registro. */
  private static LocalDateTime desde(DonacionesApi.Donacion d, String estado) {
    return d.historial() == null
        ? d.fechaRegistro()
        : d.historial().stream()
            .filter(c -> normalizado(c.estadoNuevo()).equals(estado))
            .map(Cambio::timestamp)
            .max(Comparator.naturalOrder())
            .orElse(d.fechaRegistro());
  }

  private static String nota(String estado, LocalDateTime desde) {
    return switch (estado) {
      case ASIGNADA -> "Asignada, espera el camión";
      case LISTA -> "Lista para entregar, espera el camión";
      case TRASLADO -> "Salió el " + desde.format(DIA_Y_HORA);
      case ENTREGADA -> "Entrega confirmada el " + desde.format(DIA);
      case FALLIDA -> "No pudo llegar el " + desde.format(DIA);
      case VENCIDA -> "Venció en el depósito el " + desde.format(DIA);
      default -> "En el depósito, espera destino";
    };
  }

  /** Una entidad que ya no está (404) o sin nombre queda sin destino escrito. */
  private String nombreDeEntidad(UUID id) {
    try {
      DonacionesApi.Entidad entidad = api.entidad(id);
      String nombre =
          entidad == null || entidad.juridica() == null ? null : entidad.juridica().razonSocial();
      return nombre == null || nombre.isBlank() ? null : nombre;
    } catch (HttpClientErrorException.NotFound yaNoEsta) {
      return null;
    }
  }

  private static String cantidad(int cantidad, Categoria categoria) {
    String unidad =
        categoria == null || categoria.unidad() == null ? null : UNIDADES.get(categoria.unidad());
    if (cantidad == 1 && "unidades".equals(unidad)) {
      unidad = "unidad";
    }
    return unidad == null ? String.valueOf(cantidad) : cantidad + " " + unidad;
  }

  private static String icono(String nombre) {
    String minusculas = nombre.toLowerCase(Locale.ROOT);
    return ICONOS.entrySet().stream()
        .filter(e -> minusculas.contains(e.getKey()))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElse(null);
  }
}
