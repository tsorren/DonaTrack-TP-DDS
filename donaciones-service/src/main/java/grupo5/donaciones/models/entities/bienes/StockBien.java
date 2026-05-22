package grupo5.donaciones.models.entities.bienes;

@Getter
@Setter
public class StockBien {
  private Bien bien;
  private Integer cantidadEnStock;

  // Metodos
  public void agregarStock(Integer cantidad) {
    if (cantidad == null || cantidad <= 0) {
      throw new IllegalArgumentException("La cantidad agregada al stock debe ser mayor a cero.");
    }
    this.cantidadEnStock += cantidad;
  }

  public void retirarStock(Integer cantidad) throws Exception {
    if (cantidad == null || cantidad <= 0) {
      throw new IllegalArgumentException("La cantidad retirada del stock debe ser mayor a cero.");
    }
    if (cantidad > this.cantidadEnStock) {
      throw new Exception("Stock insuficiente para realizar el retiro.");
    }
    this.cantidadEnStock -= cantidad;
  }

  public boolean hayStock() {
    return this.cantidadEnStock != null && this.cantidadEnStock > 0;
  }
}
