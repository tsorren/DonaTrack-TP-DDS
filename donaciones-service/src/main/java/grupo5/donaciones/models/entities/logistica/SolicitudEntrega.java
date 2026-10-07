package grupo5.donaciones.models.entities.logistica;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;

/**
 * Pedido de entrega de una donación a un proveedor de logística: qué proveedor la tiene, cuáles ya
 * la rechazaron y en qué estado está. Solo se puede operar mientras está {@code PENDIENTE}.
 */
@Getter
public class SolicitudEntrega implements AggregateRoot {

  private final UUID id;
  private final UUID donacionIndependienteId;
  private final LocalDateTime fechaCreacion;
  private final Set<String> proveedoresDescartados = new LinkedHashSet<>();
  private String proveedorActual;
  private EstadoSolicitudEntrega estado;

  public SolicitudEntrega(UUID donacionIndependienteId, LocalDateTime fechaCreacion) {
    if (donacionIndependienteId == null || fechaCreacion == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    this.id = UUID.randomUUID();
    this.donacionIndependienteId = donacionIndependienteId;
    this.fechaCreacion = fechaCreacion;
    this.estado = EstadoSolicitudEntrega.PENDIENTE;
  }

  public void asignarProveedor(String proveedorId) {
    exigirPendiente();
    if (proveedorId == null || proveedorId.isBlank()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    if (proveedorActual != null || proveedoresDescartados.contains(proveedorId)) {
      throw new BusinessStateException(ErrorCatalog.SOLICITUD_ENTREGA_TRANSICION_INVALIDA);
    }
    this.proveedorActual = proveedorId;
  }

  public void marcarEnviada() {
    exigirPendiente();
    if (proveedorActual == null) {
      throw new BusinessStateException(ErrorCatalog.SOLICITUD_ENTREGA_TRANSICION_INVALIDA);
    }
    this.estado = EstadoSolicitudEntrega.ENVIADA;
  }

  public void descartarProveedorActual() {
    exigirPendiente();
    if (proveedorActual == null) {
      throw new BusinessStateException(ErrorCatalog.SOLICITUD_ENTREGA_TRANSICION_INVALIDA);
    }
    proveedoresDescartados.add(proveedorActual);
    this.proveedorActual = null;
  }

  public void marcarFallida() {
    exigirPendiente();
    this.estado = EstadoSolicitudEntrega.FALLIDA;
  }

  public boolean estaActiva() {
    return estado != EstadoSolicitudEntrega.FALLIDA;
  }

  public boolean fueDescartado(String proveedorId) {
    return proveedoresDescartados.contains(proveedorId);
  }

  public Set<String> getProveedoresDescartados() {
    return Collections.unmodifiableSet(proveedoresDescartados);
  }

  private void exigirPendiente() {
    if (estado != EstadoSolicitudEntrega.PENDIENTE) {
      throw new BusinessStateException(ErrorCatalog.SOLICITUD_ENTREGA_TRANSICION_INVALIDA);
    }
  }
}
