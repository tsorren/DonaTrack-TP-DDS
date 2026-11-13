package grupo5.notificaciones.models.entities.notificaciones;

/**
 * El tipo del evento que originó una notificación (HB-09). Los nueve primeros son los nombres del
 * discriminador {@code tipo} de {@code EventoNotificableDTO}; los de importación y el de contacto
 * (R11-30) no se aceptan por REST: nacen del evento {@code importacion.terminada.v1} y del aviso de
 * mensaje de contacto. En la notificación se guarda el nombre como texto: una notificación vieja no
 * lo tiene, y un valor nuevo no rompe la lectura de las anteriores.
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
  DONACION_VENCIDA,
  IMPORTACION_TERMINADA,
  IMPORTACION_CORTADA,
  MENSAJE_CONTACTO
}
