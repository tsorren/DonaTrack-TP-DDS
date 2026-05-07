package donacion;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
@Data

public class DonacionIndependiente {
    private String descripcion;
    private List<ItemDonacion> bienes = new ArrayList<>();

    public void agregarBien(Bien bien, int cantidad) {
        ItemDonacion item = new ItemDonacion();
        item.setBien(bien);
        item.setCantidad(cantidad);
        this.bienes.add(item);
    }

    public void quitarBien(ItemDonacion bien) {
        bienes.remove(bien);
    }
}