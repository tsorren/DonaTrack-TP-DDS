package grupo5.incentivos.models.outbox;

/** Estado de una entrada del outbox. {@code FALLIDO} es terminal: requiere revisión manual. */
public enum EstadoOutbox {
  PENDIENTE,
  PUBLICADO,
  FALLIDO
}
