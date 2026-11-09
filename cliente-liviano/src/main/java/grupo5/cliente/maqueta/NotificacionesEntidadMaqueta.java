package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.NotificacionesEntidadPuerto;
import grupo5.cliente.vistas.NotificacionesEntidadVista;
import grupo5.cliente.vistas.NotificacionesEntidadVista.Novedad;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Las 6 novedades de ejemplo del Comedor Barrio Ejemplo (las mismas de {@code
 * entidad-notificaciones.html}): 3 sin leer y 3 leídas, al 15 de octubre de 2026, 18:30.
 */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.notificaciones-entidad",
    havingValue = "maqueta",
    matchIfMissing = true)
public class NotificacionesEntidadMaqueta implements NotificacionesEntidadPuerto {

  private static final String PUERTA = "Llegó a tu puerta";
  private static final String CONFIRMAR = "Confirmar recepción";
  private static final String VER_DONACION = "Ver la donación";
  private static final String ACTUALIZADO = "15 de octubre de 2026, 18:30";

  private static final List<Novedad> SIN_LEER =
      List.of(
          new Novedad(
              "n1",
              PUERTA,
              "puerta",
              "Hoy",
              "18:20",
              "2026-10-15T18:20",
              "Lentejas secas, 15 kg, llegó a tu puerta. Confirmá la recepción con una foto.",
              null,
              CONFIRMAR,
              "/entidad-seguimiento-confirmar?entrega=lentejas",
              "lentejas secas",
              true,
              false),
          new Novedad(
              "n2",
              "En camino",
              "camino",
              "Hoy",
              "9:15",
              "2026-10-15T09:15",
              "Arroz, 20 kg, salió del depósito. Llega mañana.",
              null,
              VER_DONACION,
              "/entidad-recibidas?estado=en-camino",
              "arroz",
              false,
              false),
          new Novedad(
              "n3",
              PUERTA,
              "puerta",
              "Ayer",
              "17:10",
              "2026-10-14T17:10",
              "Ropa de abrigo, 10 unidades, llegó a tu puerta. Confirmá la recepción con una foto.",
              null,
              CONFIRMAR,
              "/entidad-seguimiento-confirmar?entrega=ropa",
              "ropa de abrigo",
              true,
              false));

  private static final List<Novedad> LEIDAS =
      List.of(
          new Novedad(
              "n4",
              "Con problema",
              "problema",
              "14 de octubre",
              "16:40",
              "2026-10-14T16:40",
              "Aceite de girasol, 10 litros: el camión no pudo llegar. Te avisamos la nueva fecha.",
              null,
              "Ver tu pedido",
              "/entidad-necesidades?necesidad=aceite",
              "aceite de girasol",
              false,
              true),
          new Novedad(
              "n5",
              "Asignada",
              "salir",
              "13 de octubre",
              "10:05",
              "2026-10-13T10:05",
              "Se asignó a tu pedido de leche en polvo una donación de 12 unidades. Sale del"
                  + " depósito el 17 de octubre.",
              null,
              VER_DONACION,
              "/entidad-recibidas?estado=por-salir",
              "leche en polvo",
              false,
              true),
          new Novedad(
              "n6",
              PUERTA,
              "recibida",
              "10 de octubre",
              "11:05",
              "2026-10-10T11:05",
              "Lentejas secas, 10 kg, llegó a tu puerta.",
              "Confirmaste la recepción el 10 de octubre.",
              VER_DONACION,
              "/entidad-recibidas?estado=recibidas",
              "lentejas secas, 10 kg",
              false,
              true));

  private static final NotificacionesEntidadVista VISTA =
      new NotificacionesEntidadVista(SIN_LEER, LEIDAS, ACTUALIZADO, 3);

  private static final NotificacionesEntidadVista TODAS_LEIDAS =
      new NotificacionesEntidadVista(
          List.of(), Stream.concat(SIN_LEER.stream(), LEIDAS.stream()).toList(), ACTUALIZADO, 3);

  @Override
  public NotificacionesEntidadVista novedades(UUID entidad) {
    return VISTA;
  }

  public static NotificacionesEntidadVista todasLeidas() {
    return TODAS_LEIDAS;
  }

  @Override
  public void marcarTodasLeidas(UUID entidad) {
    // En la maqueta la vista de /entidad-notificaciones-leidas ya muestra todas como leídas.
  }
}
