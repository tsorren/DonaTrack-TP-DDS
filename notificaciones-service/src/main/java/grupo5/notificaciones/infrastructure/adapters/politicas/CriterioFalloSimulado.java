package grupo5.notificaciones.infrastructure.adapters.politicas;

public interface CriterioFalloSimulado {
  boolean debeFallar(String destinatario, String mensaje);
}
