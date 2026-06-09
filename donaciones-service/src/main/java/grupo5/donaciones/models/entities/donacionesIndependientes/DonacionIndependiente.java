package grupo5.donaciones.models.entities.donacionesIndependientes;

import grupo5.donaciones.models.entities.categorias.SubCategoria;
import java.util.ArrayList;
import java.util.List;

import grupo5.donaciones.models.entities.donaciones.Donacion;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DonacionIndependiente {
  private Donacion donacionOriginal;
  private SubCategoria subCategoria;
  private List<ItemDonacionIndependiente> items;

  public DonacionIndependiente(
      Donacion donacionOriginal,
      SubCategoria subCategoria,
      List<ItemDonacionIndependiente> items) {
    if (donacionOriginal == null) {
      throw new IllegalArgumentException("La donación original no puede ser nula.");
    }

    this.donacionOriginal = donacionOriginal;

    this.subCategoria = subCategoria;

    this.items = new ArrayList<>();
    items.forEach(this::agregarItem);
  }

  public void agregarItem(ItemDonacionIndependiente item) {
    if (item == null) {
      throw new IllegalArgumentException("El ítem a agregar no puede ser nulo.");
    }
    item.setDonacionIndependiente(this);
    this.items.add(item);
  }

  // Lanzar excepcion si el item no esta en la lista
  public void quitarItem(ItemDonacionIndependiente bien) {
    if (!this.items.contains(bien)) {
      throw new IllegalArgumentException(
          "El ítem que intenta quitar no pertenece a esta donación.");
    }
    this.items.remove(bien);
  }

  public int getCantidad() {
    return this.items.stream().mapToInt(ItemDonacionIndependiente::getCantidad).sum();
  }

  public DonacionIndependiente fragmentarse(Integer cantidadNecesitada) {
    if (this.getCantidad() <= cantidadNecesitada) {
      throw new RuntimeException();
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
}
