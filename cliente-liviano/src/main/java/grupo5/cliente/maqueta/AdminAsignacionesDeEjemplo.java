package grupo5.cliente.maqueta;

import static grupo5.cliente.vistas.AdminAsignacionesVista.MENOS_RECIBIERON;
import static grupo5.cliente.vistas.AdminAsignacionesVista.PARECIDO;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Los datos de ejemplo de Asignar donaciones (R11-24, ficha r11-p1-asignaciones): propuestas sin
 * problemas, en conflicto (por la misma donación, por la misma necesidad y una que ya no alcanza),
 * las donaciones en juego de Destino y las decididas. Nombres y donantes sintéticos. La pantalla de
 * resultado ({@code accion}) cambia lo que se ve: la aprobada y la descartada salen de la lista y
 * pasan a las decididas; después de correr con {@code ?escenario=nuevas} ({@code accion} nuevas) se
 * suman las de hoy.
 */
final class AdminAsignacionesDeEjemplo {

  static final String ARROZ_DE_ANA = "arroz-ana";

  private static final String COMEDOR = "Comedor Barrio Ejemplo";
  private static final String HOGAR = "Hogar Ejemplo Centro";
  private static final String MERENDERO = "Merendero Ejemplo Sur";
  private static final String ESCUELA = "Escuela Ejemplo N.º 3";
  private static final String APROBADA = "Aprobada";
  private static final String COMPLETA = "Cubre la necesidad completa.";
  private static final String DEL_15 = "15 de octubre";
  private static final String HOY = "hoy, 0:00";
  private static final String LIBRE = "libre";
  private static final String CONFLICTO = "conflicto";
  private static final String ARROZ = "Arroz · 25 kg (de Ana Ejemplo)";
  private static final String COMPITE_ARROZ =
      "Compite por " + ARROZ + " con 1 propuesta más: alcanza para una.";
  private static final String ELIGIO_MENOS =
      "Las donaciones son las que eligió " + MENOS_RECIBIERON + ".";
  private static final String SIN_DATO =
      "Sin el dato del algoritmo: se generó antes de que lo guardáramos.";

  private static final List<Propuesta> LIBRES =
      List.of(
          new Propuesta(
              "lentejas",
              COMEDOR,
              "Lentejas, 15 kg",
              DEL_15,
              "Lentejas · 15 kg (de Juan Ejemplo)",
              COMPLETA,
              false,
              List.of(new Sugerencia(MENOS_RECIBIERON, "1.º de 6")),
              null,
              LIBRE,
              null,
              null),
          new Propuesta(
              "leche",
              MERENDERO,
              "Leche en polvo, 12 unidades",
              DEL_15,
              "Leche en polvo · 10 unidades (de Distribuidora Ejemplo SA)",
              "Cubre 10 de 12 unidades; el resto sigue pendiente.",
              false,
              List.of(new Sugerencia(PARECIDO, "2.º de 10")),
              null,
              LIBRE,
              null,
              null),
          new Propuesta(
              "abrigos",
              HOGAR,
              "Abrigos, 10 unidades",
              DEL_15,
              "Abrigos · 10 unidades (de Ropa Ejemplo SA)",
              COMPLETA,
              false,
              List.of(
                  new Sugerencia(MENOS_RECIBIERON, "2.º de 6"),
                  new Sugerencia(PARECIDO, "4.º de 10")),
              ELIGIO_MENOS,
              LIBRE,
              null,
              null),
          new Propuesta(
              "cuadernos",
              ESCUELA,
              "Cuadernos, 40 unidades",
              "2 de octubre",
              "Cuadernos · 40 unidades (de Librería Ejemplo)",
              COMPLETA,
              false,
              List.of(),
              SIN_DATO,
              LIBRE,
              null,
              null));

  private static final List<Propuesta> NUEVAS =
      List.of(
          new Propuesta(
              "yogur",
              HOGAR,
              "Yogur, 24 unidades",
              HOY,
              "Yogur · 24 unidades (de Fundación Ejemplo Sur)",
              COMPLETA,
              true,
              List.of(new Sugerencia(PARECIDO, "1.º de 2")),
              null,
              LIBRE,
              null,
              null),
          new Propuesta(
              "utiles",
              ESCUELA,
              "Útiles escolares, 40 unidades",
              HOY,
              "Útiles escolares · 30 unidades (de Juan Ejemplo)",
              "Cubre 30 de 40 unidades; el resto sigue pendiente.",
              true,
              List.of(new Sugerencia(PARECIDO, "2.º de 2")),
              null,
              LIBRE,
              null,
              null));

  private static final List<Propuesta> EN_CONFLICTO =
      List.of(
          arroz("arroz", COMEDOR, "Arroz, 30 kg", "Cubre 25 de 30 kg; el resto sigue pendiente."),
          arroz("arroz-sur", MERENDERO, "Arroz, 25 kg", COMPLETA),
          new Propuesta(
              "fideos",
              ESCUELA,
              "Fideos, 20 kg",
              DEL_15,
              "Fideos · 20 kg (de Juan Ejemplo)",
              COMPLETA,
              false,
              List.of(new Sugerencia(PARECIDO, "5.º de 10")),
              null,
              CONFLICTO,
              "Cubre la misma necesidad que otra propuesta pendiente. Si aprobás las dos, se"
                  + " asigna de más.",
              null),
          new Propuesta(
              "azucar",
              HOGAR,
              "Azúcar, 15 kg",
              DEL_15,
              "Azúcar · 15 kg (de Ana Ejemplo)",
              COMPLETA,
              false,
              List.of(new Sugerencia(MENOS_RECIBIERON, "4.º de 6")),
              null,
              "sin-stock",
              "Ya no alcanza: Azúcar · 15 kg quedó asignada a otra organización. Podés descartarla.",
              null));

  private static final List<DonacionEnJuego> EN_JUEGO =
      List.of(
          new DonacionEnJuego(
              ARROZ_DE_ANA,
              "Arroz · 25 kg",
              "Ana Ejemplo",
              "7 días en el depósito",
              2,
              false,
              "La piden 2 propuestas y no alcanza para todas."),
          new DonacionEnJuego(
              "abrigos-ropa",
              "Abrigos · 10 unidades",
              "Ropa Ejemplo SA",
              "3 días en el depósito",
              1,
              true,
              "La pide 1 propuesta."),
          new DonacionEnJuego(
              "leche-distribuidora",
              "Leche en polvo · 10 unidades",
              "Distribuidora Ejemplo SA",
              "2 días en el depósito",
              1,
              true,
              "La pide 1 propuesta."));

  private static final String DESCARTADA = "Descartada";
  private static final String ALIMENTOS = "Alimentos";
  private static final String CUENTA_LEO = "leo";

  /**
   * Una decidida de ejemplo con lo que usa la búsqueda en memoria: la clave de la categoría, hace
   * cuántos días se decidió (-1: sin fecha) y quién ("vos", {@link #CUENTA_LEO} o "antes").
   */
  private record Ejemplo(Decidida decidida, String categoria, int dias, String autor) {}

  private static final List<Ejemplo> DECIDIDAS =
      List.of(
          new Ejemplo(
              new Decidida(
                  COMEDOR,
                  "Fideos, 20 kg",
                  APROBADA,
                  ALIMENTOS,
                  "Aprobaste vos el 11 de noviembre de 2026, 10:15.",
                  null,
                  List.of(new Cubre("fideos-ana", "Fideos · 20 kg (de Ana Ejemplo)"))),
              "alimentos",
              1,
              "vos"),
          new Ejemplo(
              new Decidida(
                  HOGAR,
                  "Aceite, 12 litros",
                  APROBADA,
                  ALIMENTOS,
                  "Aprobó Leo Gómez el 9 de noviembre de 2026, 16:02.",
                  null,
                  List.of(new Cubre("aceite-almacen", "Aceite · 12 litros (de Almacén Ejemplo)"))),
              "alimentos",
              3,
              CUENTA_LEO),
          new Ejemplo(
              new Decidida(
                  MERENDERO,
                  "Azúcar, 15 kg",
                  APROBADA,
                  ALIMENTOS,
                  "Aprobaste vos el 2 de noviembre de 2026, 9:40.",
                  null,
                  List.of(new Cubre("azucar-luis", "Azúcar · 15 kg (de Luis Ejemplo)"))),
              "alimentos",
              10,
              "vos"),
          new Ejemplo(
              new Decidida(
                  ESCUELA,
                  "Cuadernos, 40 unidades",
                  APROBADA,
                  "Útiles escolares",
                  "Aprobó Leo Gómez el 20 de octubre de 2026, 11:30.",
                  null,
                  List.of(
                      new Cubre(
                          "cuadernos-libreria", "Cuadernos · 40 unidades (de Librería Ejemplo)"))),
              "utiles",
              23,
              CUENTA_LEO),
          new Ejemplo(
              new Decidida(
                  HOGAR,
                  "Ropa de abrigo, 40 prendas",
                  APROBADA,
                  "Ropa",
                  "Se decidió antes de que guardáramos quién y cuándo.",
                  null,
                  List.of(
                      new Cubre("abrigo-marta", "Ropa de abrigo · 40 prendas (de Marta Ejemplo)"))),
              "ropa",
              -1,
              "antes"),
          new Ejemplo(
              new Decidida(
                  COMEDOR,
                  "Galletitas, 5 kg",
                  DESCARTADA,
                  ALIMENTOS,
                  "Descartó Leo Gómez el 8 de noviembre de 2026, 18:20.",
                  "El comedor ya consiguió galletitas de otra donación.",
                  List.of(new Cubre("galletitas-ana", "Galletitas · 5 kg (de Ana Ejemplo)"))),
              "alimentos",
              4,
              CUENTA_LEO),
          new Ejemplo(
              new Decidida(
                  MERENDERO,
                  "Harina, 10 kg",
                  DESCARTADA,
                  ALIMENTOS,
                  "Se decidió antes de que guardáramos quién y cuándo.",
                  "",
                  List.of(new Cubre("harina-luis", "Harina · 10 kg (de Luis Ejemplo)"))),
              "alimentos",
              -1,
              "antes"));

  private static final List<Opcion> AUTORES =
      List.of(
          new Opcion("", "Cualquiera"),
          new Opcion("vos", "Vos"),
          new Opcion(CUENTA_LEO, "Leo Gómez"),
          new Opcion("antes", "Antes de las cuentas"));

  private static final List<Opcion> CATEGORIAS =
      List.of(
          new Opcion("alimentos", "Alimentos"),
          new Opcion("ropa", "Ropa"),
          new Opcion("utiles", "Útiles escolares"));

  private AdminAsignacionesDeEjemplo() {}

  /** Todas las decididas, filtradas en memoria como lo haría Donaciones (R11-35). */
  private static List<Decidida> buscadas(ConsultaDecididas b) {
    String buscar = b.buscar().toLowerCase(Locale.ROOT);
    int dias =
        switch (b.decidida()) {
          case "hoy" -> 0;
          case "7-dias" -> 6;
          case "30-dias", "este-mes" -> 29;
          case "mes-anterior" -> -2;
          default -> Integer.MAX_VALUE;
        };
    List<Ejemplo> todas = new ArrayList<>(DECIDIDAS);
    if (ConsultaDecididas.ANTIGUAS.equals(b.orden())) {
      todas = todas.reversed();
    }
    return todas.stream()
        .filter(
            e ->
                buscar.isEmpty()
                    || (e.decidida().entidad() + " " + e.decidida().que())
                        .toLowerCase(Locale.ROOT)
                        .contains(buscar))
        .filter(
            e ->
                b.estado().isEmpty()
                    || (ConsultaDecididas.DESCARTADAS.equals(b.estado())
                        == e.decidida().descartada()))
        .filter(e -> b.categoria().isEmpty() || b.categoria().equals(e.categoria()))
        .filter(e -> b.decidio().isEmpty() || b.decidio().equals(e.autor()))
        .filter(e -> dias == Integer.MAX_VALUE || (e.dias() >= 0 && e.dias() <= dias))
        .map(Ejemplo::decidida)
        .toList();
  }

  static AdminAsignacionesVista vista(ConsultaAsignaciones consulta, String accion) {
    List<Propuesta> libres = new ArrayList<>(LIBRES);
    List<Decidida> decididas = new ArrayList<>(DECIDIDAS.stream().map(Ejemplo::decidida).toList());
    if ("aprobada".equals(accion)) {
      libres.removeIf(p -> "lentejas".equals(p.clave()));
      decididas.addFirst(
          new Decidida(
              COMEDOR,
              "Lentejas, 15 kg",
              APROBADA,
              ALIMENTOS,
              "Aprobaste vos hoy.",
              null,
              List.of()));
    }
    if ("descartada".equals(accion)) {
      libres.removeIf(p -> "leche".equals(p.clave()));
      decididas.addFirst(
          new Decidida(
              MERENDERO,
              "Leche en polvo, 12 unidades",
              DESCARTADA,
              ALIMENTOS,
              "Descartaste vos hoy.",
              null,
              List.of()));
    }
    if ("nuevas".equals(accion)) {
      libres.addAll(NUEVAS);
    }
    libres = filtradas(libres, consulta.algoritmo());
    Cuentas cuentas =
        new Cuentas((long) libres.size() + EN_CONFLICTO.size(), libres.size(), EN_CONFLICTO.size());
    String ultima = "hoy, 0:00 · generó 12 propuestas";
    int pagina = consulta.pagina();
    return switch (consulta.pestana()) {
      case ConsultaAsignaciones.DECIDIDAS ->
          new AdminAsignacionesVista(
              consulta,
              null,
              null,
              null,
              List.of(),
              Tramo.de(buscadas(consulta.decididas()), pagina, 20),
              CATEGORIAS,
              null,
              null,
              AUTORES,
              consulta.decididas().decidida().isEmpty() ? 0 : 2);
      case ConsultaAsignaciones.CONFLICTOS ->
          new AdminAsignacionesVista(
              consulta,
              cuentas,
              ultima,
              Tramo.de(EN_CONFLICTO, pagina, 10),
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
              Tramo.de(libres, pagina, 10),
              decididas.subList(0, 3),
              null,
              List.of(),
              null,
              null);
    };
  }

  private static AdminAsignacionesVista destino(
      ConsultaAsignaciones consulta, Cuentas cuentas, String ultima) {
    if (consulta.conDonacion()) {
      boolean esArroz = ARROZ_DE_ANA.equals(consulta.donacion());
      List<Propuesta> piden =
          esArroz
              ? EN_CONFLICTO.stream()
                  .filter(p -> ARROZ_DE_ANA.equals(p.comparar()))
                  .map(AdminAsignacionesDeEjemplo::sinComparar)
                  .toList()
              : List.of();
      return new AdminAsignacionesVista(
          consulta,
          cuentas,
          ultima,
          Tramo.de(piden, 1, 20),
          List.of(),
          null,
          List.of(),
          null,
          esArroz
              ? new DonacionEnJuego(
                  ARROZ_DE_ANA,
                  "Arroz · 25 kg",
                  "Ana Ejemplo",
                  "en el depósito desde el 2 de noviembre",
                  2,
                  false,
                  "La piden 2 propuestas y no alcanza para todas.")
              : null);
    }
    List<DonacionEnJuego> enJuego =
        switch (consulta.categoria()) {
          case "" -> EN_JUEGO;
          case "alimentos" -> List.of(EN_JUEGO.get(0), EN_JUEGO.get(2));
          case "ropa" -> List.of(EN_JUEGO.get(1));
          default -> List.of();
        };
    return new AdminAsignacionesVista(
        consulta,
        cuentas,
        ultima,
        null,
        List.of(),
        null,
        CATEGORIAS,
        Tramo.de(enJuego, consulta.pagina(), 10),
        null);
  }

  /** Con un algoritmo elegido, solo las que sugirió (en "Las dos", las de los dos). */
  private static List<Propuesta> filtradas(List<Propuesta> propuestas, String algoritmo) {
    return switch (algoritmo) {
      case ConsultaAsignaciones.PARECIDO -> sugeridasPor(propuestas, PARECIDO);
      case ConsultaAsignaciones.MENOS_RECIBIERON -> sugeridasPor(propuestas, MENOS_RECIBIERON);
      case ConsultaAsignaciones.LAS_DOS ->
          propuestas.stream().filter(p -> p.sugeridaPor().size() > 1).toList();
      default -> propuestas;
    };
  }

  private static List<Propuesta> sugeridasPor(List<Propuesta> propuestas, String nombre) {
    return propuestas.stream()
        .filter(p -> p.sugeridaPor().stream().anyMatch(s -> s.algoritmo().equals(nombre)))
        .toList();
  }

  private static Propuesta arroz(String clave, String entidad, String pide, String alcance) {
    return new Propuesta(
        clave,
        entidad,
        pide,
        DEL_15,
        ARROZ,
        alcance,
        false,
        List.of(
            new Sugerencia(MENOS_RECIBIERON, "arroz".equals(clave) ? "1.º de 6" : "3.º de 6"),
            new Sugerencia(PARECIDO, "arroz".equals(clave) ? "3.º de 10" : "1.º de 10")),
        ELIGIO_MENOS,
        CONFLICTO,
        COMPITE_ARROZ,
        ARROZ_DE_ANA);
  }

  /** En Destino ya se está mirando esa donación: la franja no lleva "Comparar". */
  private static Propuesta sinComparar(Propuesta p) {
    return new Propuesta(
        p.clave(),
        p.entidad(),
        p.pide(),
        p.cuando(),
        p.cubre(),
        p.alcance(),
        p.nueva(),
        p.sugeridaPor(),
        p.nota(),
        p.situacion(),
        p.franja(),
        null);
  }
}
