package grupo5.donaciones.models.entities.donacionesIndependientes;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.necesidades.Asignable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DonacionIndependiente {
  private Donacion donacionOriginal;
  private Subcategoria subCategoria;
  private List<ItemDonacionIndependiente> items;
  private EstadoDonacion estadoActual;
  private final List<CambioEstado> historial;
  private final LocalDateTime fechaRegistro;
  private Asignable asignadaA;
  private String Descripcion; //para poder decidir si me sirve la donacion para satisfacer una donacion necesito analizar semanticamente su descripcion

  public DonacionIndependiente(
      Donacion donacionOriginal, Subcategoria subCategoria, List<ItemDonacionIndependiente> items) {
    if (donacionOriginal == null) {
      throw new ValidationException(ErrorCatalog.DONACION_INDEPENDIENTE_ORIGINAL_NULA);
    }

    this.donacionOriginal = donacionOriginal;

    this.subCategoria = subCategoria;

    this.items = new ArrayList<>();
    items.forEach(this::agregarItem);

    this.estadoActual = new EnDeposito();
    this.historial = new ArrayList<>();
    this.fechaRegistro = LocalDateTime.now(ZoneId.systemDefault());
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

    return new DonacionIndependiente(this.donacionOriginal, this.subCategoria, itemsExtraidos);
  }

  // Métodos de negocio — delegan al estado actual
  public void registrar() {
    this.estadoActual.registrar(this);
  }

  public void asignar() {
    this.estadoActual.asignar(this);
  }

  public void planificarRuta() {
    this.estadoActual.planificarRuta(this);
  }

  public void iniciarRecorrido() {
    this.estadoActual.iniciarRecorrido(this);
  }

  public void confirmarEntrega() {
    this.estadoActual.confirmarEntrega(this);
  }

  public void registrarFalla(String justificacion) {
    this.estadoActual.registrarFalla(this, justificacion);
  }

  public void retornar() {
    this.estadoActual.retornar(this);
  }

  public void vencer() {
    this.estadoActual.vencer(this);
  }

  // Llamado únicamente por los estados concretos
  public void cambiarEstado(EstadoDonacion nuevoEstado, String justificacion) {
    CambioEstado cambio = new CambioEstado(this.estadoActual, nuevoEstado, justificacion);
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
