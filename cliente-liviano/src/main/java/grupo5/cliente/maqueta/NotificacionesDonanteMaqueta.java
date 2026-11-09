package grupo5.cliente.maqueta;

import static grupo5.cliente.vistas.EstadoEntrega.CON_PROBLEMA;
import static grupo5.cliente.vistas.EstadoEntrega.ENTREGADA;
import static grupo5.cliente.vistas.EstadoEntrega.EN_CAMINO;
import static grupo5.cliente.vistas.EstadoEntrega.PENDIENTE;

import grupo5.cliente.puertos.NotificacionesDonantePuerto;
import grupo5.cliente.vistas.NotificacionesDonanteVista;
import grupo5.cliente.vistas.NotificacionesDonanteVista.Noticia;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Las 17 noticias de las donaciones de ejemplo de la empresa (las mismas de {@code
 * datos-donante.html}): las 4 primeras sin leer y las 13 restantes leídas.
 */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.notificaciones-donante",
    havingValue = "maqueta",
    matchIfMissing = true)
public class NotificacionesDonanteMaqueta implements NotificacionesDonantePuerto {

  private static final String LENTEJAS = "lentejas";
  private static final String LENTEJAS_DON = "Lentejas secas, 15 kg";
  private static final String ARROZ = "arroz";
  private static final String ARROZ_DON = "Arroz largo fino, 10 kg";
  private static final String LECHE = "leche";
  private static final String LECHE_DON = "Leche en polvo, 6 kg";
  private static final String UTILES = "utiles";
  private static final String UTILES_DON = "Útiles escolares, 2 cajas";
  private static final String ROPA = "ropa";
  private static final String ROPA_DON = "Ropa de abrigo, 12 prendas";
  private static final String ZAPATILLAS = "zapatillas";
  private static final String ZAPATILLAS_DON = "Zapatillas, 4 pares";
  private static final String FIDEOS = "fideos";
  private static final String FIDEOS_DON = "Fideos secos, 8 kg";
  private static final String RECIBIMOS = "Recibimos tu donación en el depósito.";

  private static final List<Noticia> SIN_LEER =
      List.of(
          new Noticia(
              "El camión salió a entregar tus lentejas a Comedor Barrio Ejemplo.",
              "Hoy, 14:30",
              "2026-10-15T14:30",
              EN_CAMINO,
              LENTEJAS,
              LENTEJAS_DON),
          new Noticia(RECIBIMOS, "Ayer, 17:00", "2026-10-14T17:00", PENDIENTE, ARROZ, ARROZ_DON),
          new Noticia(
              "Asignamos tu donación a una necesidad de Merendero Ejemplo Sur.",
              "Ayer, 12:15",
              "2026-10-14T12:15",
              PENDIENTE,
              LECHE,
              LECHE_DON),
          new Noticia(
              "Tu donación quedó lista para entregar.",
              "Ayer, 9:10",
              "2026-10-14T09:10",
              EN_CAMINO,
              LENTEJAS,
              LENTEJAS_DON));

  private static final List<Noticia> LEIDAS =
      List.of(
          new Noticia(
              "Asignamos tu donación a una necesidad de Comedor Barrio Ejemplo.",
              "13 de octubre, 11:00",
              "2026-10-13T11:00",
              EN_CAMINO,
              LENTEJAS,
              LENTEJAS_DON),
          new Noticia(
              RECIBIMOS, "13 de octubre, 10:00", "2026-10-13T10:00", PENDIENTE, LECHE, LECHE_DON),
          new Noticia(
              RECIBIMOS,
              "12 de octubre, 10:00",
              "2026-10-12T10:00",
              EN_CAMINO,
              LENTEJAS,
              LENTEJAS_DON),
          new Noticia(
              "El camión no pudo llegar a Escuela Ejemplo N.º 12: el camino estaba cortado. Sale de"
                  + " nuevo el 17 de octubre.",
              "11 de octubre, 16:10",
              "2026-10-11T16:10",
              CON_PROBLEMA,
              UTILES,
              UTILES_DON),
          new Noticia(
              "El camión salió a entregar tus útiles escolares.",
              "11 de octubre, 9:00",
              "2026-10-11T09:00",
              CON_PROBLEMA,
              UTILES,
              UTILES_DON),
          new Noticia(
              "Merendero Ejemplo Sur confirmó que recibió tu ropa de abrigo y sumó una foto.",
              "9 de octubre, 18:05",
              "2026-10-09T18:05",
              ENTREGADA,
              ROPA,
              ROPA_DON),
          new Noticia(
              "El camión salió a entregar tu ropa de abrigo.",
              "9 de octubre, 11:40",
              "2026-10-09T11:40",
              ENTREGADA,
              ROPA,
              ROPA_DON),
          new Noticia(
              "Asignamos tu donación a una necesidad de Escuela Ejemplo N.º 12.",
              "9 de octubre, 10:20",
              "2026-10-09T10:20",
              CON_PROBLEMA,
              UTILES,
              UTILES_DON),
          new Noticia(
              RECIBIMOS,
              "6 de octubre, 15:00",
              "2026-10-06T15:00",
              PENDIENTE,
              ZAPATILLAS,
              ZAPATILLAS_DON),
          new Noticia(
              "Comedor Barrio Ejemplo confirmó que recibió tus fideos y sumó una foto.",
              "5 de octubre, 17:20",
              "2026-10-05T17:20",
              ENTREGADA,
              FIDEOS,
              FIDEOS_DON),
          new Noticia(
              "El camión salió a entregar tus fideos.",
              "5 de octubre, 10:00",
              "2026-10-05T10:00",
              ENTREGADA,
              FIDEOS,
              FIDEOS_DON),
          new Noticia(
              "Asignamos tu donación a una necesidad de Merendero Ejemplo Sur.",
              "3 de octubre, 9:30",
              "2026-10-03T09:30",
              ENTREGADA,
              ROPA,
              ROPA_DON),
          new Noticia(
              "Asignamos tu donación a una necesidad de Comedor Barrio Ejemplo.",
              "2 de octubre, 12:00",
              "2026-10-02T12:00",
              ENTREGADA,
              FIDEOS,
              FIDEOS_DON));

  private static final NotificacionesDonanteVista VISTA =
      new NotificacionesDonanteVista(SIN_LEER, LEIDAS);

  private static final NotificacionesDonanteVista TODAS_LEIDAS =
      new NotificacionesDonanteVista(
          List.of(), Stream.concat(SIN_LEER.stream(), LEIDAS.stream()).toList());

  @Override
  public NotificacionesDonanteVista notificaciones(UUID donante) {
    return VISTA;
  }

  public static NotificacionesDonanteVista todasLeidas() {
    return TODAS_LEIDAS;
  }

  @Override
  public void marcarTodasLeidas(UUID donante) {
    // En la maqueta la vista de /donante-notificaciones-leidas ya muestra todas como leídas.
  }
}
