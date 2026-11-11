package grupo5.notificaciones.models.entities.notificaciones;

/**
 * El tipo del evento que originó una notificación (HB-09). Los nombres son los del discriminador
 * {@code tipo} de {@code EventoNotificableDTO}. En la notificación se guarda el nombre como texto:
 * una notificación vieja no lo tiene, y un valor nuevo no rompe la lectura de las anteriores.
 */
public enum TipoEvento {
  DONANTE_REGISTRADO,
  DONANTE_INACTIVO,
  MISION_CUMPLIDA,
  SUBIO_CATEGORIA,
  DONACION_ASIGNADA,
  DONACION_RECIBIDA,
  DONACION_EN_CAMINO,
  ENTREGA_FALLIDA,
  DONACION_VENCIDA
}
