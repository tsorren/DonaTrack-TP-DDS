package Bienes.Atributos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class Categoria {
    private String nombre;
    private Boolean conUso;
    private Boolean conVencimiento;
    private Unidad tipoUnidad;

}
