package donacion;

import lombok.Data;

@Data
public class ItemDonacion {
  private Bien bien;
  private int cantidad;
}
