package ar.edu.utn.frba.ddsi.common;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Persona {
    private String nombre;
    private String apellido;

    public String nombreCompleto(){
        return this.nombre + " " + this.apellido;
    }
}
