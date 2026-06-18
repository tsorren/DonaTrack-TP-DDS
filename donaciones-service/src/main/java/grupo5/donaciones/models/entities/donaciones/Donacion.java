package grupo5.donaciones.models.entities.donaciones;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
import grupo5.donaciones.models.entities.donacionesIndependientes.CambioEstado;
import grupo5.donaciones.models.entities.donantes.Donante;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Donacion implements AggregateRoot {
  private final UUID id;
  private Donante donante;
  private List<ItemDonacion> items;
  private String descripcion;
  private LocalDateTime fecha;

  private Deposito depositoRecepcion;
  private EstadoDonacion estadoActual;
  private final List<CambioEstado> historialEstados;

  public Donacion(Donante donante, Deposito depositoRecepcion) {
    if (donante == null) {
      throw new ValidationException(ErrorCatalog.DONACION_SIN_DONANTE);
    }
    this.donante = donante;
    this.depositoRecepcion = depositoRecepcion;
    this.items = new ArrayList<>();
    this.estadoActual = EstadoDonacion.CARGADA;
    this.historialEstados = new ArrayList<>();
  }

  public Donacion(Donante donante) {
    this.id = UUID.randomUUID();
    this(donante, null);
  }

  public void agregarItem(ItemDonacion item) {
    if (item == null) {
      throw new ValidationException(ErrorCatalog.DONACION_ITEM_NULO);
    }
    if (this.items.contains(item)) {
      throw new ValidationException(ErrorCatalog.DONACION_ITEM_YA_AGREGADO);
    }
    this.items.add(item);
  }

  public void quitarItem(ItemDonacion item) {
    if (!this.items.contains(item)) {
      throw new ValidationException(ErrorCatalog.DONACION_ITEM_NO_PERTENECE);
    }
    this.items.remove(item);
  }

  public void marcarNormalizada() {
    if (this.estadoActual != EstadoDonacion.CARGADA) {
      throw new IllegalStateException(
          "Solo se puede normalizar una donación CARGADA. Estado actual: " + this.estadoActual);
    }
    avanzarEstado(EstadoDonacion.NORMALIZADA);
  }

  public void marcarSegmentada() {
    if (this.estadoActual != EstadoDonacion.NORMALIZADA) {
      throw new IllegalStateException(
          "Solo se puede segmentar una donación NORMALIZADA. Estado actual: " + this.estadoActual);
    }
    avanzarEstado(EstadoDonacion.SEGMENTADA);
  }

  private void avanzarEstado(EstadoDonacion nuevoEstado) {
    this.historialEstados.add(new CambioEstadoDonacion(this.estadoActual, nuevoEstado));
    this.estadoActual = nuevoEstado;
  }

  public List<CambioEstadoDonacion> getHistorialEstados() {
    return Collections.unmodifiableList(historialEstados);
  }
}
