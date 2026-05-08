package ar.edu.utn.frba.ddsi.donaciones.models.entities.bienes;

import lombok.Data;


@Data
public class StockBien {
  private Bien bien;
  private Integer cantidadEnStock;

  // Metodos
  public void agregar(Integer cantidad) {
    this.cantidadEnStock += cantidad;
  }

  public void retirar(Integer cantidad) throws Exception {
    if (cantidad > this.cantidadEnStock) {
      throw new Exception("Stock insuficiente");
    }
    this.cantidadEnStock -= cantidad;
  }

  public boolean hayStock() {
    return this.cantidadEnStock > 0;
  }
}
