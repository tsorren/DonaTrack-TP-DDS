package grupo5.donaciones.models.entities.donacionesIndependientes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.necesidades.Asignable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DonacionIndependiente implements AggregateRoot {
  private final UUID id;
  private Donacion donacionOriginal;
  private List<ItemDonacionIndependiente> items;
  private EstadoDonacion estadoActual;
  private final List<CambioEstado> historial;
  private final LocalDateTime fechaRegistro;
  @JsonIgnore private Asignable asignadaA;

  public DonacionIndependiente(Donacion donacionOriginal, List<ItemDonacionIndependiente> items) {
    this.id = UUID.randomUUID();
    this.donacionOriginal = donacionOriginal;
    if (donacionOriginal == null) {
      throw new ValidationException(ErrorCatalog.DONACION_INDEPENDIENTE_ORIGINAL_NULA);
    }

    this.items = new ArrayList<>();
    items.forEach(this::agregarItem);
    this.estadoActual = new EnDeposito();
    this.historial = new ArrayList<>();
    this.fechaRegistro = LocalDateTime.now(ZoneId.systemDefault());
  }

  public String getDescripcion() {
    StringBuilder sb = new StringBuilder();
    items.forEach(i -> sb.append(i.getBien().getBienOriginal().getDescripcion()).append(" "));
    return sb.toString();
  }

  public Subcategoria getSubcategoria() {
    return this.items.stream()
        .findFirst()
        .map(item -> item.getBien().getSubcategoria())
        .orElse(null);
  }

  public void agregarItem(ItemDonacionIndependiente item) {
    if (item == null) {
      throw new ValidationException(ErrorCatalog.DONACION_INDEPENDIENTE_AGREGAR_ITEM_NULO);
    }
    item.setDonacionIndependiente(this);
    this.items.add(item);
  }

  // Lanzar excepcion si el item no esta en la lista
  public void quitarItem(ItemDonacionIndependiente bien) {
    if (!this.items.contains(bien)) {
      throw new ValidationException(ErrorCatalog.DONACION_INDEPENDIENTE_QUITAR_ITEM_INEXISTENTE);
    }
    this.items.remove(bien);
  }

  public int getCantidad() {
    return this.items.stream().mapToInt(ItemDonacionIndependiente::getCantidad).sum();
  }

  public DonacionIndependiente fragmentarse(Integer cantidadNecesitada) {
    if (this.getCantidad() <= cantidadNecesitada) {
      throw new BusinessStateException(ErrorCatalog.FRAGMENTACION_CANTIDAD_INSUFICIENTE);
    }
    Integer cantidadPorExtraer = cantidadNecesitada;
    List<ItemDonacionIndependiente> itemsExtraidos = new ArrayList<>();
    while (cantidadPorExtraer > 0) {
      ItemDonacionIndependiente itemExtraido = this.items.getFirst();
      if (itemExtraido.getCantidad() > cantidadPorExtraer) {
        itemExtraido = itemExtraido.fragmentarse(cantidadPorExtraer);
      } else {
        this.items.remove(itemExtraido);
      }
      itemsExtraidos.add(itemExtraido);
      cantidadPorExtraer -= itemExtraido.getCantidad();
    }
    return new DonacionIndependiente(this.donacionOriginal, itemsExtraidos);
  }

  // ── Métodos de negocio ─────────────────────────────────────────────────────

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

  public void vencer(String actor) {
    this.estadoActual.vencer(this, actor);
  }

  // Llamado únicamente por los estados concretos
  public void cambiarEstado(EstadoDonacion nuevoEstado, String justificacion, String actor) {
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
