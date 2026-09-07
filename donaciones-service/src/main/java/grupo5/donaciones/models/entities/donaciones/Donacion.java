package grupo5.donaciones.models.entities.donaciones;

import grupo5.common.events.AgregadoConEventos;
import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.donaciones.events.DonacionCargada;
import grupo5.donaciones.models.entities.donaciones.events.DonacionNormalizada;
import grupo5.donaciones.models.entities.donaciones.events.DonacionSegmentada;
import grupo5.donaciones.models.entities.donaciones.events.EventoDonacion;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
public class Donacion extends AgregadoConEventos<EventoDonacion> {
  private final UUID id;

  @Setter(AccessLevel.PACKAGE)
  private UUID donanteId;

  @Getter(AccessLevel.NONE)
  private List<ItemDonacion> items;

  @Setter(AccessLevel.PACKAGE)
  private String descripcion;

  @Setter(AccessLevel.PACKAGE)
  private LocalDateTime fecha;

  @Setter(AccessLevel.PACKAGE)
  private Deposito depositoRecepcion;

  private EstadoDonacion estadoActual;
  private final List<CambioEstadoDonacion> historialEstados;

  public Donacion(
      UUID donanteId, Deposito depositoRecepcion, String descripcion, LocalDateTime fecha) {
    this(UUID.randomUUID(), donanteId, depositoRecepcion, descripcion, fecha, true);
  }

  public Donacion(
      UUID id,
      UUID donanteId,
      Deposito depositoRecepcion,
      String descripcion,
      LocalDateTime fecha) {
    this(id, donanteId, depositoRecepcion, descripcion, fecha, false);
  }

  private Donacion(
      UUID id,
      UUID donanteId,
      Deposito depositoRecepcion,
      String descripcion,
      LocalDateTime fecha,
      boolean registrarEventoDeCarga) {
    if (donanteId == null) {
      throw new ValidationException(ErrorCatalog.DONACION_SIN_DONANTE);
    }
    if (id == null) {
      throw new IllegalArgumentException("El id de la donación no puede ser nulo");
    }
    this.id = id;
    this.donanteId = donanteId;
    this.depositoRecepcion = depositoRecepcion;
    this.descripcion = descripcion;
    this.fecha = fecha != null ? fecha : LocalDateTime.now(java.time.ZoneId.of("UTC"));
    this.items = new ArrayList<>();
    this.estadoActual = EstadoDonacion.CARGADA;
    this.historialEstados = new ArrayList<>();
    if (registrarEventoDeCarga) {
      this.registrarEvento(new DonacionCargada(this.id, this.donanteId));
    }
  }

  public Donacion(UUID donanteId, Deposito depositoRecepcion) {
    this(donanteId, depositoRecepcion, null, null);
  }

  public Donacion(UUID donanteId) {
    this(donanteId, null, null, null);
  }

  public List<ItemDonacion> getItems() {
    return Collections.unmodifiableList(items);
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
      throw new BusinessStateException(ErrorCatalog.ESTADO_DONACION_TRANSICION_INVALIDA);
    }
    avanzarEstado(EstadoDonacion.NORMALIZADA);
    this.registrarEvento(new DonacionNormalizada(this.id, this.donanteId));
  }

  public void marcarSegmentada() {
    if (this.estadoActual != EstadoDonacion.NORMALIZADA) {
      throw new BusinessStateException(ErrorCatalog.ESTADO_DONACION_TRANSICION_INVALIDA);
    }
    avanzarEstado(EstadoDonacion.SEGMENTADA);
    this.registrarEvento(new DonacionSegmentada(this.id, this.donanteId));
  }

  private void avanzarEstado(EstadoDonacion nuevoEstado) {
    this.historialEstados.add(new CambioEstadoDonacion(this.estadoActual, nuevoEstado));
    this.estadoActual = nuevoEstado;
  }

  public List<CambioEstadoDonacion> getHistorialEstados() {
    return Collections.unmodifiableList(historialEstados);
  }
}
