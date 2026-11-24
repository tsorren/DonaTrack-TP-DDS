package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.DonanteInicioPuerto;
import grupo5.cliente.vistas.DonanteAnioVista;
import grupo5.cliente.vistas.DonanteDonacionVista;
import grupo5.cliente.vistas.DonanteDonacionVista.Carta;
import grupo5.cliente.vistas.DonanteInicioVista;
import grupo5.cliente.vistas.DonanteInicioVista.Aviso;
import grupo5.cliente.vistas.DonanteTableroVista;
import grupo5.cliente.vistas.EstadoEntrega;
import grupo5.cliente.vistas.PasoRecorrido;
import grupo5.cliente.vistas.PasoRecorrido.Avance;
import grupo5.cliente.vistas.Tramo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * El inicio de la empresa de ejemplo: las siete donaciones de viaje-datos, las tres noticias más
 * nuevas (las de datos-donante) y las fechas de la maqueta ("hoy" es el 15 de octubre de 2026). Con
 * la persona de ejemplo ({@code ?cuenta=persona}, leída como en {@link DonantePerfilMaqueta}), el
 * inicio es el de Martina Ruiz: sus ocho donaciones, copiadas de
 * templates/persona/datos-persona.html (las que siguen leyendo su detalle y su seguimiento), una
 * por mes desde junio y cuatro en octubre; el año de la persona sale de esas ocho. El tablero corta
 * las siete de la empresa por columna con {@link Tramo#de}; con {@code ?escenario=muchas}, suma
 * donaciones de ejemplo solo al tablero (1, 5 y 48, los totales del boceto A) para ver los
 * paginadores, sin cambiar las cifras ni el hito del inicio. Con {@code ?escenario=sin-camino}, el
 * inicio va sin la donación en camino (A04 sin lámina protagonista; la persona, con la huerta en su
 * lugar).
 */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.donante",
    havingValue = "maqueta",
    matchIfMissing = true)
class DonanteInicioMaqueta implements DonanteInicioPuerto {

  private static final DonanteInicioVista INICIO =
      new DonanteInicioVista(
          DonanteDonacionesDeEjemplo.DONACIONES,
          List.of(
              new Aviso(donacion("lentejas"), 0),
              new Aviso(donacion("arroz"), 0),
              new Aviso(donacion("leche"), 0)),
          4,
          2,
          "octubre de 2026",
          "15 de octubre",
          "15 de octubre, 16:25",
          "1 de octubre",
          "2026-10-15",
          "2026-10-14");

  /** El escenario de la maqueta sin nada en camino: la lámina protagonista lo dice. */
  private static final String SIN_CAMINO = "sin-camino";

  private static final DonanteInicioVista INICIO_SIN_CAMINO = sinCamino(INICIO);

  private static final String DEPOSITO = "En depósito";
  private static final String ASIGNADA = "Asignada";
  private static final String LISTA = "Lista para entregar";
  private static final String CAMINO_PASO = "En camino";
  private static final String ENTREGADA_PASO = "Entregada";
  private static final String ALIMENTOS = "cat-alimentos";
  private static final String TEXTO_ALIMENTOS = "Alimentos";
  private static final String ROPA = "cat-ropa";
  private static final String TEXTO_ROPA = "Ropa";
  private static final String ESCOLARES = "cat-escolares";
  private static final String COMEDOR = "Comedor Barrio Ejemplo";
  private static final String MERENDERO = "Merendero Ejemplo Sur";
  private static final String FOTO = "/img/ejemplo-entrega.svg";
  private static final String RECIBIMOS = "Recibimos tu donación en el depósito.";
  private static final String PERSONA = "persona";
  private static final String CUENTA = "cuenta";
  private static final String DE_2026 = " de 2026";
  private static final String NUEVE_OCT = "9 de octubre";
  private static final String CATORCE_OCT = "14 de octubre";

  /** Los meses del año de la maqueta, hasta octubre (antes de las donaciones, que los leen). */
  private static final List<String> MESES =
      List.of(
          "enero",
          "febrero",
          "marzo",
          "abril",
          "mayo",
          "junio",
          "julio",
          "agosto",
          "septiembre",
          "octubre");

  /** Las ocho donaciones de Martina Ruiz, de la más nueva a la más vieja (datos-persona.html). */
  private static final List<DonanteDonacionVista> DE_MARTINA =
      List.of(
          new DonanteDonacionVista(
              "fideos",
              8,
              "Fideos secos",
              "3 kg",
              ALIMENTOS,
              TEXTO_ALIMENTOS,
              null,
              "14 de octubre de 2026",
              null,
              EstadoEntrega.PENDIENTE,
              "En el depósito, espera destino",
              null,
              null,
              null,
              pasos(0, CATORCE_OCT),
              List.of(new Carta(RECIBIMOS, "14 de octubre, 18:10", "2026-10-14T18:10"))),
          new DonanteDonacionVista(
              "leche",
              7,
              "Leche en polvo",
              "4 unidades",
              ALIMENTOS,
              TEXTO_ALIMENTOS,
              COMEDOR,
              "13 de octubre de 2026",
              null,
              EstadoEntrega.PENDIENTE,
              "Asignada, espera el camión",
              null,
              null,
              "Sale del depósito el 16 de octubre.",
              pasos(1, "13 de octubre", CATORCE_OCT),
              List.of(
                  new Carta(
                      "Asignamos tu leche en polvo a una necesidad de " + COMEDOR + ".",
                      "14 de octubre, 12:40",
                      "2026-10-14T12:40"),
                  new Carta(RECIBIMOS, "13 de octubre, 11:30", "2026-10-13T11:30"))),
          new DonanteDonacionVista(
              "ropa",
              6,
              "Ropa de abrigo",
              "12 prendas",
              ROPA,
              TEXTO_ROPA,
              MERENDERO,
              "7 de octubre de 2026",
              null,
              EstadoEntrega.EN_CAMINO,
              "Salió hoy a las 11:10",
              null,
              null,
              "El camión ya pasó la mitad del camino. Posición aproximada, actualizada hoy a las"
                  + " 16:25.",
              pasos(3, "7 de octubre", NUEVE_OCT, CATORCE_OCT, "Hoy, 11:10"),
              List.of(
                  new Carta(
                      "El camión salió a entregar tu ropa de abrigo a " + MERENDERO + ".",
                      "Hoy, 11:10",
                      "2026-10-15T11:10"),
                  new Carta(
                      "Tu ropa de abrigo quedó lista para entregar.",
                      "14 de octubre, 15:00",
                      "2026-10-14T15:00"),
                  new Carta(
                      "Asignamos tu ropa de abrigo a una necesidad de " + MERENDERO + ".",
                      "9 de octubre, 10:05",
                      "2026-10-09T10:05"),
                  new Carta(RECIBIMOS, "7 de octubre, 17:30", "2026-10-07T17:30"))),
          entregada(
              new Entregada("arroz", 5, "Arroz", "5 kg", ALIMENTOS, COMEDOR, "tu arroz", FOTO),
              List.of("2 de octubre", "5 de octubre", "8 de octubre", NUEVE_OCT),
              List.of("13:15", "9:20", "10:00"),
              "Los paquetes de arroz sobre la mesa de la cocina del comedor"),
          entregada(
              new Entregada(
                  "utiles",
                  4,
                  "Útiles escolares",
                  "1 caja",
                  ESCOLARES,
                  "Escuela Ejemplo N.º 12",
                  "tus útiles escolares",
                  FOTO),
              List.of("2 de septiembre", "3 de septiembre", "7 de septiembre", "8 de septiembre"),
              List.of("12:30", "9:00", "11:15"),
              "La caja de útiles abierta sobre un pupitre de la escuela"),
          entregada(
              new Entregada(
                  "galletitas",
                  3,
                  "Galletitas",
                  "2 kg",
                  ALIMENTOS,
                  MERENDERO,
                  "tus galletitas",
                  FOTO),
              List.of("3 de agosto", "4 de agosto", "6 de agosto", "7 de agosto"),
              List.of("17:40", "10:30", "9:50"),
              "Los paquetes de galletitas en un estante del merendero"),
          entregada(
              new Entregada(
                  "frazadas",
                  2,
                  "Frazadas",
                  "2 unidades",
                  ROPA,
                  "Hogar Casa Ejemplo",
                  "tus frazadas",
                  "/img/ejemplo-entrega-ropa.svg"),
              List.of("6 de julio", "7 de julio", "9 de julio", "10 de julio"),
              List.of("16:05", "9:45", "12:00"),
              "Las dos frazadas dobladas sobre un sillón del hogar"),
          entregada(
              new Entregada(
                  "lentejas",
                  1,
                  "Lentejas secas",
                  "4 kg",
                  ALIMENTOS,
                  COMEDOR,
                  "tus lentejas",
                  FOTO),
              List.of("5 de junio", "6 de junio", "8 de junio", "9 de junio"),
              List.of("15:20", "9:10", "10:40"),
              "La bolsa de lentejas junto a la olla grande del comedor"));

  /** El inicio de Martina, con las fechas de la empresa: 1 entrega confirmada en octubre. */
  private static final DonanteInicioVista DE_LA_PERSONA =
      new DonanteInicioVista(
          DE_MARTINA,
          avisos(DE_MARTINA),
          3,
          1,
          INICIO.mes(),
          INICIO.hoy(),
          INICIO.actualizado(),
          "5 de junio",
          INICIO.hoyIso(),
          INICIO.ayerIso());

  private static final DonanteInicioVista DE_LA_PERSONA_SIN_CAMINO = sinCamino(DE_LA_PERSONA);

  private static final DonanteAnioVista ANIO =
      new DonanteAnioVista(
          2026,
          MESES.size(),
          IntStream.rangeClosed(1, MESES.size()).mapToObj(DonanteInicioMaqueta::mes).toList(),
          DE_MARTINA.size(),
          contar(EstadoEntrega.ENTREGADA),
          contar(EstadoEntrega.EN_CAMINO),
          contar(EstadoEntrega.PENDIENTE),
          true,
          DE_LA_PERSONA.avisos(),
          DE_LA_PERSONA.sinLeer(),
          INICIO.hoy(),
          INICIO.actualizado(),
          INICIO.hoyIso(),
          INICIO.ayerIso());

  private static final Predicate<DonanteDonacionVista> TENER =
      d -> d.estado() == EstadoEntrega.CON_PROBLEMA;
  private static final Predicate<DonanteDonacionVista> CAMINO =
      d ->
          d.estado() == EstadoEntrega.EN_CAMINO
              || (d.estado() == EstadoEntrega.PENDIENTE && d.destino() != null);
  private static final Predicate<DonanteDonacionVista> LLEGO =
      d -> d.estado() == EstadoEntrega.ENTREGADA;

  /** El escenario de la maqueta que llena el tablero. */
  private static final String MUCHAS = "muchas";

  /** Nombres de ejemplo para las copias de {@code ?escenario=muchas}. */
  private static final List<String> OTRAS =
      List.of(
          "Aceite de girasol",
          "Frazadas",
          "Polenta",
          "Cuadernos",
          "Yerba",
          "Camperas",
          "Azúcar",
          "Lápices");

  private static final List<DonanteDonacionVista> MUCHAS_TENER = estiradas(TENER, 1);
  private static final List<DonanteDonacionVista> MUCHAS_CAMINO = estiradas(CAMINO, 5);
  private static final List<DonanteDonacionVista> MUCHAS_LLEGO = estiradas(LLEGO, 48);

  /**
   * Lo que cambia entre las entregadas de Martina: la clave, el número, qué y cuánto, la categoría,
   * la organización, cómo la nombran las cartas ("tu arroz") y la foto.
   */
  private record Entregada(
      String clave,
      long numero,
      String titulo,
      String cantidad,
      String categoria,
      String destino,
      String nombrada,
      String foto) {}

  private static List<DonanteDonacionVista> de(Predicate<DonanteDonacionVista> columna) {
    return INICIO.donaciones().stream().filter(columna).toList();
  }

  /**
   * Las de la columna estiradas hasta {@code total}: las de ejemplo primero y después copias con
   * otros nombres (cada una lleva al detalle de la que copia).
   */
  private static List<DonanteDonacionVista> estiradas(
      Predicate<DonanteDonacionVista> columna, int total) {
    List<DonanteDonacionVista> propias = de(columna);
    Stream<DonanteDonacionVista> copias =
        IntStream.range(0, total - propias.size())
            .mapToObj(i -> copia(propias.get(i % propias.size()), OTRAS.get(i % OTRAS.size())));
    return Stream.concat(propias.stream(), copias).toList();
  }

  private static DonanteDonacionVista copia(DonanteDonacionVista d, String titulo) {
    return new DonanteDonacionVista(
        d.clave(),
        d.numero(),
        titulo,
        d.cantidad(),
        d.categoria(),
        d.categoriaTexto(),
        d.destino(),
        d.donada(),
        d.llego(),
        d.estado(),
        d.nota(),
        d.foto(),
        d.fotoAlt(),
        d.seguimiento(),
        d.pasos(),
        d.cartas());
  }

  private static DonanteInicioVista sinCamino(DonanteInicioVista v) {
    return new DonanteInicioVista(
        v.donaciones().stream().filter(d -> d.estado() != EstadoEntrega.EN_CAMINO).toList(),
        v.avisos(),
        v.sinLeer(),
        v.entregadasDelMes(),
        v.mes(),
        v.hoy(),
        v.actualizado(),
        v.desde(),
        v.hoyIso(),
        v.ayerIso());
  }

  /**
   * Las noticias de las donaciones, de la más nueva a la más vieja, como las ordena el adaptador.
   */
  private static List<Aviso> avisos(List<DonanteDonacionVista> donaciones) {
    return donaciones.stream()
        .flatMap(d -> IntStream.range(0, d.cartas().size()).mapToObj(i -> new Aviso(d, i)))
        .sorted(Comparator.comparing((Aviso a) -> a.d().cartas().get(a.i()).fecha()).reversed())
        .toList();
  }

  /** Los 5 pasos: hasta {@code actual}, hechos; el actual; los que siguen, pendientes. */
  private static List<PasoRecorrido> pasos(int actual, String... detalles) {
    List<String> nombres = List.of(DEPOSITO, ASIGNADA, LISTA, CAMINO_PASO, ENTREGADA_PASO);
    return IntStream.range(0, nombres.size())
        .mapToObj(
            i -> {
              if (i > actual) {
                return new PasoRecorrido(nombres.get(i), "Pendiente", Avance.PENDIENTE);
              }
              return new PasoRecorrido(
                  nombres.get(i), detalles[i], i < actual ? Avance.HECHO : Avance.ACTUAL);
            })
        .toList();
  }

  /**
   * Una entregada de Martina. {@code dias}: depósito, asignada, lista y entregada (el camión sale y
   * llega el mismo día, como en datos-persona.html). {@code horas}: las de la confirmación, la
   * salida y la asignación.
   */
  private static DonanteDonacionVista entregada(
      Entregada e, List<String> dias, List<String> horas, String fotoAlt) {
    String llego = dias.get(3);
    String asignada = dias.get(1);
    return new DonanteDonacionVista(
        e.clave(),
        e.numero(),
        e.titulo(),
        e.cantidad(),
        e.categoria(),
        texto(e.categoria()),
        e.destino(),
        dias.get(0) + DE_2026,
        llego + DE_2026,
        EstadoEntrega.ENTREGADA,
        "Llegó el " + llego,
        e.foto(),
        fotoAlt,
        null,
        List.of(
            new PasoRecorrido(DEPOSITO, dias.get(0), Avance.HECHO),
            new PasoRecorrido(ASIGNADA, asignada, Avance.HECHO),
            new PasoRecorrido(LISTA, dias.get(2), Avance.HECHO),
            new PasoRecorrido(CAMINO_PASO, llego, Avance.HECHO),
            new PasoRecorrido(ENTREGADA_PASO, llego, Avance.HECHO)),
        List.of(
            new Carta(
                e.destino() + " confirmó que recibió " + e.nombrada() + " y sumó una foto.",
                llego + ", " + horas.get(0),
                iso(llego, horas.get(0))),
            new Carta(
                "El camión salió a entregar " + e.nombrada() + ".",
                llego + ", " + horas.get(1),
                iso(llego, horas.get(1))),
            new Carta(
                "Asignamos " + e.nombrada() + " a una necesidad de " + e.destino() + ".",
                asignada + ", " + horas.get(2),
                iso(asignada, horas.get(2)))));
  }

  private static String texto(String categoria) {
    return switch (categoria) {
      case ROPA -> TEXTO_ROPA;
      case ESCOLARES -> "Útiles escolares";
      default -> TEXTO_ALIMENTOS;
    };
  }

  /** "9 de octubre" y "9:20" dan "2026-10-09T09:20" (todas las de Martina son de 2026). */
  private static String iso(String dia, String hora) {
    String[] partes = dia.split(" de ");
    String[] hm = hora.split(":");
    return "2026-%02d-%02dT%02d:%s"
        .formatted(
            MESES.indexOf(partes[1]) + 1,
            Integer.parseInt(partes[0]),
            Integer.parseInt(hm[0]),
            hm[1]);
  }

  private static DonanteAnioVista.Mes mes(int numero) {
    String nombre = MESES.get(numero - 1);
    List<DonanteDonacionVista> delMes =
        DE_MARTINA.stream().filter(d -> d.donada().endsWith(nombre + DE_2026)).toList();
    return new DonanteAnioVista.Mes(
        numero,
        nombre,
        "2026-%02d".formatted(numero),
        delMes.size(),
        delMes.stream().limit(DonanteAnioVista.POR_MES).toList());
  }

  private static long contar(EstadoEntrega estado) {
    return DE_MARTINA.stream().filter(d -> d.estado() == estado).count();
  }

  private static DonanteDonacionVista donacion(String clave) {
    return DonanteDonacionesDeEjemplo.DONACIONES.stream()
        .filter(d -> d.clave().equals(clave))
        .findFirst()
        .orElseThrow();
  }

  @Override
  public DonanteInicioVista inicio(UUID donante) {
    boolean sinCamino = SIN_CAMINO.equals(escenario());
    if (esPersona()) {
      return sinCamino ? DE_LA_PERSONA_SIN_CAMINO : DE_LA_PERSONA;
    }
    return sinCamino ? INICIO_SIN_CAMINO : INICIO;
  }

  @Override
  public DonanteTableroVista tablero(UUID donante, int tener, int camino, int llego) {
    boolean muchas = MUCHAS.equals(escenario());
    int porColumna = DonanteTableroVista.POR_COLUMNA;
    return new DonanteTableroVista(
        Tramo.de(muchas ? MUCHAS_TENER : de(TENER), tener, porColumna),
        Tramo.de(muchas ? MUCHAS_CAMINO : de(CAMINO), camino, porColumna),
        Tramo.de(muchas ? MUCHAS_LLEGO : de(LLEGO), llego, porColumna),
        INICIO.donaciones().stream()
            .filter(d -> d.estado() == EstadoEntrega.PENDIENTE && d.destino() == null)
            .count());
  }

  private static String escenario() {
    HttpServletRequest pedido = pedido();
    return pedido == null ? null : pedido.getParameter("escenario");
  }

  /** La sesión eligió la persona de ejemplo: primero el parámetro, después la sesión. */
  private static boolean esPersona() {
    HttpServletRequest pedido = pedido();
    if (pedido == null) {
      return false;
    }
    String elegida = pedido.getParameter(CUENTA);
    if (elegida != null) {
      return PERSONA.equals(elegida);
    }
    HttpSession sesion = pedido.getSession(false);
    return sesion != null && PERSONA.equals(sesion.getAttribute(CUENTA));
  }

  private static HttpServletRequest pedido() {
    return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes pedido
        ? pedido.getRequest()
        : null;
  }

  /** El año de la persona de ejemplo, Martina Ruiz: solo lo lee su inicio. */
  @Override
  public DonanteAnioVista anio(UUID donante) {
    return ANIO;
  }
}
