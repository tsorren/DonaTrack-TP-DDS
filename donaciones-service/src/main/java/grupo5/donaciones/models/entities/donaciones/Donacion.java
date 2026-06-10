package grupo5.donaciones.models.entities.donaciones;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Donacion {
  private Donante donante;
  private List<ItemDonacion> items;
  private String descripcion;
  private LocalDateTime fecha;

  public Donacion(Donante donante) {
    this.items = new ArrayList<>();
    if (donante == null) {
      throw new ValidationException(ErrorCatalog.DONACION_SIN_DONANTE);
    }

    this.donante = donante;
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

  // Lanzar excepcion si la donacion independiente no esta en la lista
  public void quitarItem(ItemDonacion item) {
    if (!this.items.contains(item)) {
      throw new ValidationException(ErrorCatalog.DONACION_ITEM_NO_PERTENECE);
    }

    this.items.remove(item);
  }
}
