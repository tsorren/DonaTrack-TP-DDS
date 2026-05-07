package Bienes;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class StockBien {
    private Bien bien;
    private int cantidadEnStock;

    // Metodos
    public void agregar(int cantidad) {
        this.cantidadEnStock += cantidad;
    }

    public void retirar(int cantidad) throws Exception {
        if (cantidad > this.cantidadEnStock) {
            throw new Exception("Stock insuficiente");
        }
        this.cantidadEnStock -= cantidad;
    }

    public boolean hayStock() {
        return this.cantidadEnStock > 0;
    }
}