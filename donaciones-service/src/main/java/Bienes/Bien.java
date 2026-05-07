package Bienes;

import Bienes.Atributos.Estado;
import Bienes.Atributos.Subcategoria;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class Bien {
    private String descripcion;
    private String foto;
    private LocalDate fechaVencimiento;
    private Estado estado;
    private Subcategoria subcategoria;

    // metodos
    public boolean estaVencido() {
        if (this.fechaVencimiento == null) return false;
        return this.fechaVencimiento.isBefore(LocalDate.now());
    }
}
