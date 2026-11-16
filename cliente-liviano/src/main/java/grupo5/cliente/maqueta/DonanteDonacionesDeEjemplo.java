package grupo5.cliente.maqueta;

import static grupo5.cliente.vistas.EstadoEntrega.CON_PROBLEMA;
import static grupo5.cliente.vistas.EstadoEntrega.ENTREGADA;
import static grupo5.cliente.vistas.EstadoEntrega.EN_CAMINO;
import static grupo5.cliente.vistas.EstadoEntrega.PENDIENTE;

import grupo5.cliente.vistas.DonanteDonacionVista;
import grupo5.cliente.vistas.DonanteDonacionVista.Carta;
import grupo5.cliente.vistas.PasoRecorrido;
import grupo5.cliente.vistas.PasoRecorrido.Avance;
import java.util.List;

/**
 * Las siete donaciones de ejemplo de la empresa, copiadas de templates/viaje-datos.html (que siguen
 * leyendo las pantallas de la persona): dos entregadas a dos entidades, una en camino, tres por
 * salir y una con el camino cortado. "Hoy" es el 15 de octubre de 2026. Las usan el inicio y
 * Seguimiento de la maqueta.
 */
final class DonanteDonacionesDeEjemplo {

  private static final String ALIMENTOS = "cat-alimentos";
  private static final String TEXTO_ALIMENTOS = "Alimentos";
  private static final String VESTIMENTA = "cat-vestimenta";
  private static final String TEXTO_VESTIMENTA = "Vestimenta";
  private static final String MERENDERO = "Merendero Ejemplo Sur";
  private static final String COMEDOR = "Comedor Barrio Ejemplo";
  private static final String ESCUELA = "Escuela Ejemplo 12";
  private static final String FOTO = "/img/ejemplo-entrega.svg";
  private static final String DEPOSITO = "En depósito";
  private static final String ASIGNADA = "Asignada";
  private static final String LISTA = "Lista para entregar";
  private static final String CAMINO = "En camino";
  private static final String ENTREGADA_PASO = "Entregada";
  private static final String PENDIENTE_TEXTO = "Pendiente";
  private static final String RECIBIMOS = "Recibimos tu donación en el depósito.";
  private static final String ASIGNAMOS = "Asignamos tu donación a una necesidad de ";
  private static final String NUEVE = "9 de octubre";
  private static final String CINCO = "5 de octubre";

  static final List<DonanteDonacionVista> DONACIONES =
      List.of(
          new DonanteDonacionVista(
              "arroz",
              7,
              "Arroz largo fino",
              "10 kg",
              ALIMENTOS,
              TEXTO_ALIMENTOS,
              null,
              "14 de octubre de 2026",
              null,
              PENDIENTE,
              "En el depósito, espera destino",
              null,
              null,
              null,
              List.of(
                  actual(DEPOSITO, "14 de octubre"),
                  falta(ASIGNADA),
                  falta(LISTA),
                  falta(CAMINO),
                  falta(ENTREGADA_PASO)),
              List.of(new Carta(RECIBIMOS, "14 de octubre, 17:00", "2026-10-14T17:00"))),
          new DonanteDonacionVista(
              "leche",
              6,
              "Leche en polvo",
              "6 kg",
              ALIMENTOS,
              TEXTO_ALIMENTOS,
              MERENDERO,
              "13 de octubre de 2026",
              null,
              PENDIENTE,
              "Asignada, espera el camión",
              null,
              null,
              "Sale del depósito el 16 de octubre.",
              List.of(
                  hecho(DEPOSITO, "13 de octubre"),
                  actual(ASIGNADA, "14 de octubre"),
                  falta(LISTA),
                  falta(CAMINO),
                  falta(ENTREGADA_PASO)),
              List.of(
                  new Carta(
                      ASIGNAMOS + MERENDERO + ".", "14 de octubre, 12:15", "2026-10-14T12:15"),
                  new Carta(RECIBIMOS, "13 de octubre, 10:00", "2026-10-13T10:00"))),
          new DonanteDonacionVista(
              "lentejas",
              5,
              "Lentejas secas",
              "15 kg",
              ALIMENTOS,
              TEXTO_ALIMENTOS,
              COMEDOR,
              "12 de octubre de 2026",
              null,
              EN_CAMINO,
              "Salió hoy a las 14:30",
              null,
              null,
              "El camión va por la mitad del camino. Posición aproximada, actualizada hoy a las"
                  + " 16:25.",
              List.of(
                  hecho(DEPOSITO, "12 de octubre"),
                  hecho(ASIGNADA, "13 de octubre"),
                  hecho(LISTA, "14 de octubre"),
                  actual(CAMINO, "Hoy, 14:30"),
                  falta(ENTREGADA_PASO)),
              List.of(
                  new Carta(
                      "El camión salió a entregar tus lentejas a " + COMEDOR + ".",
                      "Hoy, 14:30",
                      "2026-10-15T14:30"),
                  new Carta(
                      "Tu donación quedó lista para entregar.",
                      "14 de octubre, 9:10",
                      "2026-10-14T09:10"),
                  new Carta(ASIGNAMOS + COMEDOR + ".", "13 de octubre, 11:00", "2026-10-13T11:00"),
                  new Carta(RECIBIMOS, "12 de octubre, 10:00", "2026-10-12T10:00"))),
          new DonanteDonacionVista(
              "utiles",
              4,
              "Útiles escolares",
              "2 cajas",
              "cat-escolares",
              "Útiles escolares",
              ESCUELA,
              "8 de octubre de 2026",
              null,
              CON_PROBLEMA,
              "No pudo llegar; sale de nuevo el 17 de octubre",
              null,
              null,
              "El camino estaba cortado. Última noticia: 11 de octubre, 16:10.",
              List.of(
                  hecho(DEPOSITO, "8 de octubre"),
                  hecho(ASIGNADA, NUEVE),
                  hecho(LISTA, "10 de octubre"),
                  actual(CAMINO, "11 de octubre: no pudo llegar"),
                  new PasoRecorrido(
                      ENTREGADA_PASO, "Nueva salida: 17 de octubre", Avance.PENDIENTE)),
              List.of(
                  new Carta(
                      "El camión no pudo llegar a "
                          + ESCUELA
                          + ": el camino estaba cortado. Sale de nuevo el 17 de octubre.",
                      "11 de octubre, 16:10",
                      "2026-10-11T16:10"),
                  new Carta(
                      "El camión salió a entregar tus útiles escolares.",
                      "11 de octubre, 9:00",
                      "2026-10-11T09:00"),
                  new Carta(ASIGNAMOS + ESCUELA + ".", "9 de octubre, 10:20", "2026-10-09T10:20"))),
          new DonanteDonacionVista(
              "zapatillas",
              3,
              "Zapatillas",
              "4 pares",
              VESTIMENTA,
              TEXTO_VESTIMENTA,
              null,
              "6 de octubre de 2026",
              null,
              PENDIENTE,
              "Espera una necesidad que le corresponda",
              null,
              null,
              null,
              List.of(
                  actual(DEPOSITO, "6 de octubre"),
                  falta(ASIGNADA),
                  falta(LISTA),
                  falta(CAMINO),
                  falta(ENTREGADA_PASO)),
              List.of(new Carta(RECIBIMOS, "6 de octubre, 15:00", "2026-10-06T15:00"))),
          entregada(
              "ropa",
              2,
              "Ropa de abrigo",
              "12 prendas",
              VESTIMENTA,
              TEXTO_VESTIMENTA,
              MERENDERO,
              "2 de octubre de 2026",
              "9 de octubre de 2026",
              "Llegó el 9 de octubre",
              "Cajas con la ropa de abrigo junto a la puerta del merendero",
              List.of(
                  hecho(DEPOSITO, "2 de octubre"),
                  hecho(ASIGNADA, "3 de octubre"),
                  hecho(LISTA, "6 de octubre"),
                  hecho(CAMINO, NUEVE),
                  hecho(ENTREGADA_PASO, NUEVE)),
              List.of(
                  new Carta(
                      MERENDERO + " confirmó que recibió tu ropa de abrigo y sumó una foto.",
                      "9 de octubre, 18:05",
                      "2026-10-09T18:05"),
                  new Carta(
                      "El camión salió a entregar tu ropa de abrigo.",
                      "9 de octubre, 11:40",
                      "2026-10-09T11:40"),
                  new Carta(
                      ASIGNAMOS + MERENDERO + ".", "3 de octubre, 9:30", "2026-10-03T09:30"))),
          entregada(
              "fideos",
              1,
              "Fideos secos",
              "8 kg",
              ALIMENTOS,
              TEXTO_ALIMENTOS,
              COMEDOR,
              "1 de octubre de 2026",
              "5 de octubre de 2026",
              "Llegó el 5 de octubre",
              "Cajas con los fideos apiladas junto a la puerta del comedor",
              List.of(
                  hecho(DEPOSITO, "1 de octubre"),
                  hecho(ASIGNADA, "2 de octubre"),
                  hecho(LISTA, "3 de octubre"),
                  hecho(CAMINO, CINCO),
                  hecho(ENTREGADA_PASO, CINCO)),
              List.of(
                  new Carta(
                      COMEDOR + " confirmó que recibió tus fideos y sumó una foto.",
                      "5 de octubre, 17:20",
                      "2026-10-05T17:20"),
                  new Carta(
                      "El camión salió a entregar tus fideos.",
                      "5 de octubre, 10:00",
                      "2026-10-05T10:00"),
                  new Carta(
                      ASIGNAMOS + COMEDOR + ".", "2 de octubre, 12:00", "2026-10-02T12:00"))));

  private DonanteDonacionesDeEjemplo() {}

  @SuppressWarnings("java:S107") // Una entregada de ejemplo, con sus datos de la maqueta.
  private static DonanteDonacionVista entregada(
      String clave,
      long numero,
      String titulo,
      String cantidad,
      String categoria,
      String categoriaTexto,
      String destino,
      String donada,
      String llego,
      String nota,
      String fotoAlt,
      List<PasoRecorrido> pasos,
      List<Carta> cartas) {
    return new DonanteDonacionVista(
        clave,
        numero,
        titulo,
        cantidad,
        categoria,
        categoriaTexto,
        destino,
        donada,
        llego,
        ENTREGADA,
        nota,
        FOTO,
        fotoAlt,
        null,
        pasos,
        cartas);
  }

  private static PasoRecorrido hecho(String nombre, String detalle) {
    return new PasoRecorrido(nombre, detalle, Avance.HECHO);
  }

  private static PasoRecorrido actual(String nombre, String detalle) {
    return new PasoRecorrido(nombre, detalle, Avance.ACTUAL);
  }

  private static PasoRecorrido falta(String nombre) {
    return new PasoRecorrido(nombre, PENDIENTE_TEXTO, Avance.PENDIENTE);
  }
}
