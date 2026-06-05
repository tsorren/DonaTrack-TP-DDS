package grupo5.donaciones.models.entities.donaciones.segmentaciones;

import grupo5.donaciones.models.entities.bienes.SubCategoria;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DonacionIndependiente {
  private DonacionSegmentada donacionSegmentada;
  private SubCategoria subCategoria;
  private List<ItemDonacionIndependiente> items;

  // Constructor para inicialización lazy (sin DonacionSegmentada)
  public DonacionIndependiente(SubCategoria subCategoria, List<ItemDonacionIndependiente> items) {
    if (subCategoria == null) {
      throw new IllegalArgumentException("La subcategoría no puede ser nula.");
    }
    if (items == null) {
      throw new IllegalArgumentException("La lista de items no puede ser nula.");
    }

    subCategoria.agregarDonacion(this);

    this.items = new ArrayList<>();
    items.forEach(this::agregarItem);
  }

  // Constructor completo (con DonacionSegmentada)

  public DonacionIndependiente(
      DonacionSegmentada donacionSegmentada,
      SubCategoria subCategoria,
      List<ItemDonacionIndependiente> items) {
    if (donacionSegmentada == null) {
      throw new IllegalArgumentException("La donación segmentada no puede ser nula.");
    }
    donacionSegmentada.agregarDonacionIndependiente(this);
    subCategoria.agregarDonacion(this);

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
    // Usar constructor lazy para la nueva donación
    DonacionIndependiente donacionIndependienteExtraida =
        new DonacionIndependiente(this.subCategoria, itemsExtraidos);

    // Si esta donación pertenece a una DonacionSegmentada, vincular la nueva también
    if (this.donacionSegmentada != null) {
      this.donacionSegmentada.agregarDonacionIndependiente(donacionIndependienteExtraida);
    }

    return donacionIndependienteExtraida;
  }
}
