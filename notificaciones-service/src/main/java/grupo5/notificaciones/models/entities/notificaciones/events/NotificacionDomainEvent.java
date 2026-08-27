package grupo5.notificaciones.models.entities.notificaciones.events;

/**
 * Marca los eventos de dominio que {@code Notificacion} genera sobre sí misma (Oleada 2, RF-02).
 *
 * <p>common-lib no tiene todavía una base común para agregados con eventos (ej. un {@code
 * AgregadoConEventos<T>} genérico) — {@code AggregateRoot} solo define {@code getId()}. Esta
 * interfaz, y el mecanismo de {@code domainEvents}/{@code getDomainEvents()}/{@code
 * clearDomainEvents()} de {@code Notificacion}, son candidatos a migrar a common-lib el día que
 * otro servicio construya esa base compartida (ver Fase 0.5 de la auditoría de este servicio).
 */
public interface NotificacionDomainEvent {}
