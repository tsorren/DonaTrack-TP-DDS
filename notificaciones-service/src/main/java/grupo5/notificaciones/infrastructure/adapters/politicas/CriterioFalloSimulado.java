package grupo5.notificaciones.infrastructure.adapters.politicas;

/**
 * Estrategia (Strategy Pattern) para determinar si un intento de notificación simulada debe fallar
 * de forma controlada para propósitos de testing y verificación del ciclo de vida
 * (EstadoNotificacion.FALLIDA).
 */
@FunctionalInterface
public interface CriterioFalloSimulado {

  /**
   * Evalúa si el envío hacia el destinatario con el mensaje dado debe simular un fallo.
   *
   * @param destinatario Dirección de correo, teléfono o identificador de destino.
   * @param mensaje Contenido del mensaje a notificar.
   * @return {@code true} si se debe simular una falla (retornando {@code false} en el adapter), o
   *     {@code false} si la entrega debe considerarse exitosa.
   */
  boolean debeFallar(String destinatario, String mensaje);
}
