package grupo5.notificaciones.infrastructure.adapters.politicas;

/** Strategy para determinar si una notificación debe fallar de forma permanente (HTTP 400). */
@FunctionalInterface
public interface CriterioFalloSimulado {
  boolean debeFallar(String destinatario, String mensaje);
}
