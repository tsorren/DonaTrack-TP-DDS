package ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Atributos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class categoria {
    private String nombre;
    private Boolean conUso;
    private Boolean conVencimiento;
    private unidad tipoUnidad;

}
