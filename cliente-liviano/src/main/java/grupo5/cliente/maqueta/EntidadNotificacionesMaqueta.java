package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.EntidadNotificacionesPuerto;
import grupo5.cliente.vistas.EntidadNotificacionesVista;
import grupo5.cliente.vistas.EntidadNotificacionesVista.Novedad;
import grupo5.cliente.vistas.EntidadNotificacionesVista.Tema;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Las ocho novedades del Comedor Barrio Ejemplo. "Ahora" es el 15 de octubre de 2026, 18:30; los
 * ejemplos cubren minutos, horas, días, semanas y el mes anterior. Marcar no guarda nada: en la
 * maqueta el formulario lo atiende SitioController.
 */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.entidad",
    havingValue = "maqueta",
    matchIfMissing = true)
class EntidadNotificacionesMaqueta implements EntidadNotificacionesPuerto {

  private static final String PUERTA = "puerta";
  private static final String CAMINO = "camino";
  private static final String NECESIDADES = "necesidades";
  private static final String RECIBIDAS = "recibidas";
  private static final String CONFIRMAR = "Confirmar recepción";
  private static final String A_RECIBIDAS = "/entidad-recibidas?estado=recibidas";
  private static final String A_EN_CAMINO = "/entidad-recibidas?estado=en-camino";

  private static final EntidadNotificacionesVista BANDEJA =
      new EntidadNotificacionesVista(
          List.of(
              new Novedad(
                  "n1",
                  PUERTA,
                  "2026-10-15T18:20",
                  false,
                  "Lentejas secas, 15 kg, llegó a tu puerta. Confirmá la recepción con una foto.",
                  CONFIRMAR,
                  "lentejas secas",
                  "/entidad-seguimiento-confirmar?entrega=lentejas",
                  null),
              new Novedad(
                  "n2",
                  CAMINO,
                  "2026-10-15T09:15",
                  false,
                  "Arroz, 20 kg, salió del depósito. Llega mañana.",
                  null,
                  null,
                  A_EN_CAMINO,
                  null),
              new Novedad(
                  "n3",
                  PUERTA,
                  "2026-10-14T17:10",
                  false,
                  "Ropa de abrigo, 10 unidades, llegó a tu puerta. Confirmá la recepción con una"
                      + " foto.",
                  CONFIRMAR,
                  "ropa de abrigo",
                  "/entidad-seguimiento-confirmar?entrega=ropa",
                  null),
              new Novedad(
                  "n4",
                  "problemas",
                  "2026-10-14T16:40",
                  true,
                  "Aceite de girasol, 10 litros: el camión no pudo llegar. Te avisamos la nueva"
                      + " fecha.",
                  null,
                  null,
                  "/entidad-necesidades?necesidad=aceite",
                  null),
              new Novedad(
                  "n5",
                  NECESIDADES,
                  "2026-10-13T10:05",
                  true,
                  "Se asignó a tu necesidad de leche en polvo una donación de 12 unidades. Sale"
                      + " del depósito el 17 de octubre.",
                  null,
                  null,
                  "/entidad-recibidas?estado=por-salir",
                  null),
              new Novedad(
                  "n6",
                  RECIBIDAS,
                  "2026-10-10T11:05",
                  true,
                  "Lentejas secas, 10 kg, llegó a tu puerta.",
                  null,
                  null,
                  A_RECIBIDAS,
                  "Confirmaste la recepción el 10 de octubre."),
              new Novedad(
                  "n7",
                  NECESIDADES,
                  "2026-10-07T12:00",
                  true,
                  "Se asignó a tu necesidad de arroz una donación de 20 kg.",
                  null,
                  null,
                  A_EN_CAMINO,
                  null),
              new Novedad(
                  "n8",
                  RECIBIDAS,
                  "2026-09-28T12:00",
                  true,
                  "Fideos secos, 10 kg, llegó a tu puerta.",
                  null,
                  null,
                  A_RECIBIDAS,
                  "Confirmaste la recepción el 28 de septiembre.")),
          List.of(
              new Tema("", "Todas las categorías", null),
              new Tema(CAMINO, "Entregas en camino", "camion"),
              new Tema(PUERTA, "En tu puerta", PUERTA),
              new Tema(RECIBIDAS, "Donaciones recibidas", "tilde"),
              new Tema(NECESIDADES, "Necesidades", "necesidad"),
              new Tema("problemas", "Problemas", "problema")),
          "2026-10-15T18:30",
          "15 de octubre de 2026, 18:30");

  @Override
  public EntidadNotificacionesVista bandeja(UUID entidad) {
    return BANDEJA;
  }

  @Override
  public void marcarLeidas(UUID entidad, List<UUID> ids) {
    // En la maqueta no se guarda nada.
  }

  @Override
  public void dejarSinLeer(UUID id) {
    // En la maqueta no se guarda nada.
  }
}
