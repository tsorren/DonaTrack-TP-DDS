package grupo5.donaciones.models.entities.donacionesIndependientes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import grupo5.common.events.AgregadoConEventos;
import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.donacionesIndependientes.events.EventoDonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.Asignable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.Getter;

@Getter
public class DonacionIndependiente extends AgregadoConEventos<EventoDonacionIndependiente> {
  private final UUID id;
  private UUID donacionOriginalId;
  private List<ItemDonacionIndependiente> items;
  private EstadoDonacionIndependiente estadoActual;

  void setEstadoActual(EstadoDonacionIndependiente estadoActual) {
    this.estadoActual = estadoActual;
  }

  private final List<CambioEstado> historial;
  private final LocalDateTime fechaRegistro;
  @JsonIgnore private Asignable asignadaA;

  public DonacionIndependiente(UUID donacionOriginalId, List<ItemDonacionIndependiente> items) {
    if (donacionOriginalId == null) {
      throw new ValidationException(ErrorCatalog.DONACION_INDEPENDIENTE_ORIGINAL_NULA);
    }
    this.id = UUID.randomUUID();
    this.donacionOriginalId = donacionOriginalId;

    this.items = new ArrayList<>();
    items.forEach(this::agregarItem);
    this.estadoActual = new EnDeposito();
    this.historial = new ArrayList<>();
    this.fechaRegistro = LocalDateTime.now(ZoneId.of("UTC"));
  }

  public String getDescripcion() {
    StringBuilder sb = new StringBuilder();
    items.forEach(i -> sb.append(i.bien().bienOriginal().descripcion()).append(" "));
    return sb.toString();
  }

  public UUID getSubcategoriaId() {
    return this.items.stream().findFirst().map(item -> item.bien().subcategoriaId()).orElse(null);
  }

  public void agregarItem(ItemDonacionIndependiente item) {
    if (item == null) {
      throw new ValidationException(ErrorCatalog.DONACION_INDEPENDIENTE_AGREGAR_ITEM_NULO);
    }
    this.items.add(item);
  }

  public void quitarItem(ItemDonacionIndependiente bien) {
    if (!this.items.contains(bien)) {
      throw new ValidationException(ErrorCatalog.DONACION_INDEPENDIENTE_QUITAR_ITEM_INEXISTENTE);
    }
    this.items.remove(bien);
  }

  public int getCantidad() {
    return this.items.stream().mapToInt(ItemDonacionIndependiente::cantidad).sum();
  }

  public DonacionIndependiente fragmentarse(Integer cantidadNecesitada) {
    if (this.getCantidad() <= cantidadNecesitada) {
      throw new BusinessStateException(ErrorCatalog.FRAGMENTACION_CANTIDAD_INSUFICIENTE);
    }
    Integer cantidadPorExtraer = cantidadNecesitada;
    List<ItemDonacionIndependiente> itemsExtraidos = new ArrayList<>();
    while (cantidadPorExtraer > 0) {
      ItemDonacionIndependiente itemExtraido = this.items.getFirst();
      if (itemExtraido.cantidad() > cantidadPorExtraer) {
        ItemDonacionIndependiente remainder =
            new ItemDonacionIndependiente(
                itemExtraido.bien(), itemExtraido.cantidad() - cantidadPorExtraer);
        itemExtraido = itemExtraido.fragmentarse(cantidadPorExtraer);
        this.items.set(0, remainder);
      } else {
        this.items.remove(itemExtraido);
      }
      itemsExtraidos.add(itemExtraido);
      cantidadPorExtraer -= itemExtraido.cantidad();
    }
    return new DonacionIndependiente(this.donacionOriginalId, itemsExtraidos);
  }

  public Double getPesoTotal() {
    return items.stream().mapToDouble(ItemDonacionIndependiente::getPesoTotal).sum();
  }

  public Double getVolumenTotal() {
    return items.stream().mapToDouble(ItemDonacionIndependiente::getVolumenTotal).sum();
  }

  public Boolean estaVencida() {
    return this.items.stream().anyMatch(i -> i.bien().estaVencido());
  }

  // ── Métodos de negocio ─────────────────────────────────────────────────────

  public void cambiarEstado(SolicitudCambioEstadoDonacionIndependiente solicitud) {
    if (solicitud == null || solicitud.getEstado() == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    switch (solicitud.getEstado()) {
      case ASIGNACION_REALIZADA -> this.estadoActual.asignar(this, solicitud);
      case LISTA_PARA_ENTREGAR -> this.estadoActual.planificarRuta(this, solicitud);
      case EN_TRASLADO -> this.estadoActual.iniciarRecorrido(this, solicitud);
      case ENTREGADA -> this.estadoActual.confirmarEntrega(this, solicitud);
      case ENTREGA_FALLIDA -> this.estadoActual.registrarFalla(this, solicitud);
      case EN_DEPOSITO -> this.estadoActual.retornar(this, solicitud);
      case VENCIDA -> this.estadoActual.vencer(this, solicitud);
      default -> throw new BusinessStateException(ErrorCatalog.ESTADO_DONACION_TRANSICION_INVALIDA);
    }
  }

  public void registrar(String actor) {
    this.estadoActual.registrar(this, actor);
  }

  public void asignar(String actor, Asignable receptor) {
    this.asignadaA = receptor;
    this.estadoActual.asignar(this, actor);
  }

  public void planificarRuta(String actor) {
    this.estadoActual.planificarRuta(this, actor);
  }

  public void iniciarRecorrido(String actor) {
    this.estadoActual.iniciarRecorrido(this, actor);
  }

  public void confirmarEntrega(String actor) {
    this.estadoActual.confirmarEntrega(this, actor);
  }

  public void registrarFalla(String justificacion, String actor) {
    this.estadoActual.registrarFalla(this, justificacion, actor);
  }

  public void retornar(String actor) {
    this.estadoActual.retornar(this, actor);
  }

  public void replanificar(String actor) {
    this.estadoActual.replanificar(this, actor);
  }

  public void vencer(String actor) {
    this.estadoActual.vencer(this, actor);
  }

  public void asignarReceptor(Asignable receptor) {
    this.asignadaA = receptor;
  }

  // Llamado únicamente por los estados concretos
  void cambiarEstado(EstadoDonacionIndependiente nuevoEstado, String justificacion, String actor) {
    CambioEstado cambio = new CambioEstado(this.estadoActual, nuevoEstado, justificacion, actor);
    this.historial.add(cambio);
    this.estadoActual = nuevoEstado;
  }

  public List<CambioEstado> getHistorial() {
    return Collections.unmodifiableList(historial);
  }

  public Asignable asignadaA() {
    return this.asignadaA;
  }
}
