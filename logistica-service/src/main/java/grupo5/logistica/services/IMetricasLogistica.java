package grupo5.logistica.services;

/**
 * Hechos de negocio de Logística que se miden; quién los cuenta y cómo lo decide la
 * infraestructura.
 */
public interface IMetricasLogistica {

  void planificacionProcesada();

  void planificacionFallida();
}
