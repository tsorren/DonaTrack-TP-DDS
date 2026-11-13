package grupo5.donaciones.models.repositories.listados;

import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;

/**
 * Orden de {@code GET /donaciones-independientes}: por fecha de registro, como {@link
 * OrdenPorFecha}, {@code ATENCION} (R11-43), lo que la organización tiene que atender primero, o
 * {@code VENCE} (R11-34), el vencimiento más próximo de sus bienes primero y las que no vencen al
 * final. Desempata por la fecha de registro, la más reciente primero, y por id.
 */
public enum OrdenDonacionesIndependientes {
  RECIENTES,
  ANTIGUAS,
  /**
   * Por grupos: en la puerta; con problema (entrega fallida y vencida); en camino; por salir
   * (asignada, lista para entregar y en depósito); recibidas. Dentro de cada grupo, la más reciente
   * primero.
   */
  ATENCION,
  /** El vencimiento más próximo de sus bienes primero; las que no vencen, al final. */
  VENCE;

  /** El grupo de {@code ATENCION} de un estado: 0 es el primero. */
  public static int grupoDeAtencion(TipoEstadoDonacion estado) {
    if (estado == null) {
      return 5;
    }
    return switch (estado) {
      case EN_PUERTA -> 0;
      case ENTREGA_FALLIDA, VENCIDA -> 1;
      case EN_TRASLADO -> 2;
      case ASIGNACION_REALIZADA, LISTA_PARA_ENTREGAR, EN_DEPOSITO -> 3;
      case ENTREGADA -> 4;
    };
  }
}
