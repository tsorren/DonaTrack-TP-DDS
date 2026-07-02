package grupo5.logistica.services;

/**
 * Orquesta la planificacion diferida de entregas. El scheduler debe depender de este contrato y no
 * contener logica de negocio ni de persistencia.
 */
public interface IPlanificacionService {

  /** Genera las solicitudes y rutas necesarias para la siguiente jornada operativa. */
  void solicitarPlanificacionParaSiguienteJornada();
}
