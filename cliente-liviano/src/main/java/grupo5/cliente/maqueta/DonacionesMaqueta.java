package grupo5.cliente.maqueta;

import static grupo5.cliente.vistas.EstadoEntrega.CON_PROBLEMA;
import static grupo5.cliente.vistas.EstadoEntrega.ENTREGADA;
import static grupo5.cliente.vistas.EstadoEntrega.EN_CAMINO;
import static grupo5.cliente.vistas.EstadoEntrega.PENDIENTE;

import grupo5.cliente.puertos.DonacionesPuerto;
import grupo5.cliente.vistas.DetalleDonacionVista;
import grupo5.cliente.vistas.DetalleDonacionVista.Noticia;
import grupo5.cliente.vistas.DetalleDonacionVista.Problema;
import grupo5.cliente.vistas.MisDonacionesVista;
import grupo5.cliente.vistas.MisDonacionesVista.Categoria;
import grupo5.cliente.vistas.MisDonacionesVista.Donacion;
import grupo5.cliente.vistas.PasoRecorrido;
import grupo5.cliente.vistas.PasoRecorrido.Avance;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Las siete donaciones de ejemplo de la empresa, las mismas de {@code viaje-datos.html} (que siguen
 * usando el detalle, el inicio y Seguimiento), ya con los textos que muestra la pantalla. "Hoy" es
 * el 15 de octubre de 2026.
 */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.donaciones",
    havingValue = "maqueta",
    matchIfMissing = true)
class DonacionesMaqueta implements DonacionesPuerto {

  private static final String ALIMENTOS = "alimentos";
  private static final String ICONO_ALIMENTOS = "cat-alimentos";
  private static final String TEXTO_ALIMENTOS = "Alimentos";
  private static final String ROPA = "ropa";
  private static final String ICONO_ROPA = "cat-ropa";
  private static final String TEXTO_ROPA = "Ropa";
  private static final String UTILES = "Útiles escolares";
  private static final String MERENDERO = "Merendero Ejemplo Sur";
  private static final String COMEDOR = "Comedor Barrio Ejemplo";
  private static final String DEPOSITO = "En depósito";
  private static final String ASIGNADA = "Asignada";
  private static final String LISTA = "Lista para entregar";
  private static final String CAMINO = "En camino";
  private static final String ENTREGADA_PASO = "Entregada";
  private static final String RECIBIMOS = "Recibimos tu donación en el depósito.";
  private static final String ASIGNAMOS = "Asignamos tu donación a una necesidad de ";
  private static final String ESCUELA = "Escuela Ejemplo N.º 12";
  private static final String FOTO = "/img/ejemplo-entrega.svg";
  private static final String EMPRESA = "Distribuidora Ejemplo SA";
  private static final String LENTEJAS = "lentejas";
  private static final String NUEVE_DE_OCTUBRE = "9 de octubre";

  private static final MisDonacionesVista DONACIONES =
      new MisDonacionesVista(
          List.of(
              new Donacion(
                  "arroz",
                  7,
                  "Arroz largo fino",
                  "10 kg",
                  ALIMENTOS,
                  ICONO_ALIMENTOS,
                  TEXTO_ALIMENTOS,
                  null,
                  "14 de octubre de 2026",
                  PENDIENTE,
                  "En el depósito, espera destino"),
              new Donacion(
                  "leche",
                  6,
                  "Leche en polvo",
                  "6 kg",
                  ALIMENTOS,
                  ICONO_ALIMENTOS,
                  TEXTO_ALIMENTOS,
                  MERENDERO,
                  "13 de octubre de 2026",
                  PENDIENTE,
                  "Asignada, espera el camión"),
              new Donacion(
                  LENTEJAS,
                  5,
                  "Lentejas secas",
                  "15 kg",
                  ALIMENTOS,
                  ICONO_ALIMENTOS,
                  TEXTO_ALIMENTOS,
                  COMEDOR,
                  "12 de octubre de 2026",
                  EN_CAMINO,
                  "Salió hoy a las 14:30"),
              new Donacion(
                  "utiles",
                  4,
                  UTILES,
                  "2 cajas",
                  "escolares",
                  "cat-escolares",
                  UTILES,
                  ESCUELA,
                  "8 de octubre de 2026",
                  CON_PROBLEMA,
                  "No pudo llegar; sale de nuevo el 17 de octubre"),
              new Donacion(
                  "zapatillas",
                  3,
                  "Zapatillas",
                  "4 pares",
                  ROPA,
                  ICONO_ROPA,
                  TEXTO_ROPA,
                  null,
                  "6 de octubre de 2026",
                  PENDIENTE,
                  "Espera una necesidad que le corresponda"),
              new Donacion(
                  ROPA,
                  2,
                  "Ropa de abrigo",
                  "12 prendas",
                  ROPA,
                  ICONO_ROPA,
                  TEXTO_ROPA,
                  MERENDERO,
                  "2 de octubre de 2026",
                  ENTREGADA,
                  "Entrega confirmada el 9 de octubre, con foto de la organización"),
              new Donacion(
                  "fideos",
                  1,
                  "Fideos secos",
                  "8 kg",
                  ALIMENTOS,
                  ICONO_ALIMENTOS,
                  TEXTO_ALIMENTOS,
                  COMEDOR,
                  "1 de octubre de 2026",
                  ENTREGADA,
                  "Entrega confirmada el 5 de octubre, con foto de la organización")),
          // Con sus subcategorías de ejemplo (RF-05, R11-13): la maqueta filtra por su categoría
          List.of(
              new Categoria(
                  ALIMENTOS,
                  TEXTO_ALIMENTOS,
                  List.of(
                      new Categoria("arroz", "Arroz"),
                      new Categoria("fideos", "Fideos"),
                      new Categoria(LENTEJAS, "Legumbres"),
                      new Categoria("leche", "Leche en polvo"))),
              new Categoria(
                  ROPA,
                  TEXTO_ROPA,
                  List.of(
                      new Categoria("abrigo", "Ropa de abrigo"),
                      new Categoria("calzado", "Calzado"))),
              new Categoria(
                  "escolares", UTILES, List.of(new Categoria("utiles-escolares", UTILES)))),
          "hoy, 15 de octubre, 16:25");

  /** Lo que suma el detalle a cada donación, como en viaje-datos ("hoy" es el 15 de octubre). */
  private static final Map<String, DetalleDonacionVista> DETALLES =
      Map.of(
          "arroz",
          detalle(
              "arroz",
              null,
              List.of(
                  actual(DEPOSITO, "14 de octubre"),
                  falta(ASIGNADA),
                  falta(LISTA),
                  falta(CAMINO),
                  falta(ENTREGADA_PASO)),
              null,
              null,
              List.of(new Noticia("Ayer, 17:00", "2026-10-14T17:00", RECIBIMOS))),
          "leche",
          detalle(
              "leche",
              null,
              List.of(
                  hecho(DEPOSITO, "13 de octubre"),
                  actual(ASIGNADA, "14 de octubre"),
                  falta(LISTA),
                  falta(CAMINO),
                  falta(ENTREGADA_PASO)),
              null,
              null,
              List.of(
                  new Noticia("Ayer, 12:15", "2026-10-14T12:15", ASIGNAMOS + MERENDERO + "."),
                  new Noticia("13 de octubre, 10:00", "2026-10-13T10:00", RECIBIMOS))),
          LENTEJAS,
          detalle(
              LENTEJAS,
              null,
              List.of(
                  hecho(DEPOSITO, "12 de octubre"),
                  hecho(ASIGNADA, "13 de octubre"),
                  hecho(LISTA, "14 de octubre"),
                  actual(CAMINO, "Hoy, 14:30"),
                  falta(ENTREGADA_PASO)),
              null,
              null,
              List.of(
                  new Noticia(
                      "Hoy, 14:30",
                      "2026-10-15T14:30",
                      "El camión salió a entregar tus lentejas a " + COMEDOR + "."),
                  new Noticia(
                      "Ayer, 9:10", "2026-10-14T09:10", "Tu donación quedó lista para entregar."),
                  new Noticia(
                      "13 de octubre, 11:00", "2026-10-13T11:00", ASIGNAMOS + COMEDOR + "."),
                  new Noticia("12 de octubre, 10:00", "2026-10-12T10:00", RECIBIMOS))),
          "utiles",
          detalle(
              "utiles",
              null,
              List.of(
                  hecho(DEPOSITO, "8 de octubre"),
                  hecho(ASIGNADA, NUEVE_DE_OCTUBRE),
                  hecho(LISTA, "10 de octubre"),
                  actual(CAMINO, "11 de octubre: no pudo llegar"),
                  new PasoRecorrido(
                      ENTREGADA_PASO, "Nueva salida: 17 de octubre", Avance.PENDIENTE)),
              new Problema(
                  "No pudo llegar a " + ESCUELA + ".",
                  "El camino estaba cortado. Sale de nuevo el 17 de octubre. Última noticia: 11 de"
                      + " octubre, 16:10."),
              null,
              List.of(
                  new Noticia(
                      "11 de octubre, 16:10",
                      "2026-10-11T16:10",
                      "El camión no pudo llegar a "
                          + ESCUELA
                          + ": el camino estaba cortado. Sale de nuevo el 17 de octubre."),
                  new Noticia(
                      "11 de octubre, 9:00",
                      "2026-10-11T09:00",
                      "El camión salió a entregar tus útiles escolares."),
                  new Noticia(
                      "9 de octubre, 10:20", "2026-10-09T10:20", ASIGNAMOS + ESCUELA + "."))),
          "zapatillas",
          detalle(
              "zapatillas",
              null,
              List.of(
                  actual(DEPOSITO, "6 de octubre"),
                  falta(ASIGNADA),
                  falta(LISTA),
                  falta(CAMINO),
                  falta(ENTREGADA_PASO)),
              null,
              null,
              List.of(new Noticia("6 de octubre, 15:00", "2026-10-06T15:00", RECIBIMOS))),
          ROPA,
          detalle(
              ROPA,
              "9 de octubre de 2026",
              List.of(
                  hecho(DEPOSITO, "2 de octubre"),
                  hecho(ASIGNADA, "3 de octubre"),
                  hecho(LISTA, "6 de octubre"),
                  hecho(CAMINO, NUEVE_DE_OCTUBRE),
                  hecho(ENTREGADA_PASO, NUEVE_DE_OCTUBRE)),
              null,
              new Foto(
                  "Cajas con la ropa de abrigo junto a la puerta del merendero",
                  MERENDERO + " confirmó la entrega el 9 de octubre y subió esta foto."),
              List.of(
                  new Noticia(
                      "9 de octubre, 18:05",
                      "2026-10-09T18:05",
                      MERENDERO + " confirmó que recibió tu ropa de abrigo y sumó una foto."),
                  new Noticia(
                      "9 de octubre, 11:40",
                      "2026-10-09T11:40",
                      "El camión salió a entregar tu ropa de abrigo."),
                  new Noticia(
                      "3 de octubre, 9:30", "2026-10-03T09:30", ASIGNAMOS + MERENDERO + "."))),
          "fideos",
          detalle(
              "fideos",
              "5 de octubre de 2026",
              List.of(
                  hecho(DEPOSITO, "1 de octubre"),
                  hecho(ASIGNADA, "2 de octubre"),
                  hecho(LISTA, "3 de octubre"),
                  hecho(CAMINO, "5 de octubre"),
                  hecho(ENTREGADA_PASO, "5 de octubre")),
              null,
              new Foto(
                  "Cajas con los fideos apiladas junto a la puerta del comedor",
                  COMEDOR + " confirmó la entrega el 5 de octubre y subió esta foto."),
              List.of(
                  new Noticia(
                      "5 de octubre, 17:20",
                      "2026-10-05T17:20",
                      COMEDOR + " confirmó que recibió tus fideos y sumó una foto."),
                  new Noticia(
                      "5 de octubre, 10:00",
                      "2026-10-05T10:00",
                      "El camión salió a entregar tus fideos."),
                  new Noticia(
                      "2 de octubre, 12:00", "2026-10-02T12:00", ASIGNAMOS + COMEDOR + "."))));

  /** La foto de ejemplo de una entregada: su texto alternativo y su pie. */
  private record Foto(String alt, String pie) {}

  @Override
  public MisDonacionesVista misDonaciones(UUID donante) {
    return DONACIONES;
  }

  /** Con 4 por página, las siete de ejemplo ocupan dos y el paginador se ve y se comprueba. */
  @Override
  public int porPagina() {
    return 4;
  }

  @Override
  public Optional<DetalleDonacionVista> detalle(UUID donante, String clave) {
    return Optional.ofNullable(DETALLES.get(clave == null ? LENTEJAS : clave));
  }

  private static DetalleDonacionVista detalle(
      String clave,
      String llego,
      List<PasoRecorrido> pasos,
      Problema problema,
      Foto foto,
      List<Noticia> noticias) {
    Donacion donacion =
        DONACIONES.donaciones().stream()
            .filter(d -> d.clave().equals(clave))
            .findFirst()
            .orElseThrow();
    return new DetalleDonacionVista(
        donacion,
        llego,
        pasos,
        problema,
        foto == null ? null : FOTO,
        foto == null ? null : foto.alt(),
        foto == null ? null : foto.pie(),
        noticias,
        EMPRESA);
  }

  private static PasoRecorrido hecho(String nombre, String detalle) {
    return new PasoRecorrido(nombre, detalle, Avance.HECHO);
  }

  private static PasoRecorrido actual(String nombre, String detalle) {
    return new PasoRecorrido(nombre, detalle, Avance.ACTUAL);
  }

  private static PasoRecorrido falta(String nombre) {
    return new PasoRecorrido(nombre, "Pendiente", Avance.PENDIENTE);
  }
}
