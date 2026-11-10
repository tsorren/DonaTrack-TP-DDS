package grupo5.donaciones.models.entities.donacionesIndependientes;

import grupo5.donaciones.models.entities.necesidades.DestinoAsignacion;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Datos de la raíz con los que se reconstituye una {@link DonacionIndependiente} leída de la base
 * (mismo criterio que {@code DatosDonacion}). {@code destino} es {@code null} si no está asignada.
 * {@code version} es la del bloqueo optimista: el dominio no la usa, solo la lleva de vuelta al
 * guardar. {@code cancelacionPedida} es la marca de HB-19 (la necesidad se dio de baja y se espera
 * la confirmación de Logística).
 */
public record DatosDonacionIndependiente(
    UUID id,
    UUID donacionOriginalId,
    EstadoDonacionIndependiente estadoActual,
    LocalDateTime fechaRegistro,
    DestinoAsignacion destino,
    Long version,
    boolean cancelacionPedida) {

  public DatosDonacionIndependiente(
      UUID id,
      UUID donacionOriginalId,
      EstadoDonacionIndependiente estadoActual,
      LocalDateTime fechaRegistro,
      DestinoAsignacion destino,
      Long version) {
    this(id, donacionOriginalId, estadoActual, fechaRegistro, destino, version, false);
  }
}
