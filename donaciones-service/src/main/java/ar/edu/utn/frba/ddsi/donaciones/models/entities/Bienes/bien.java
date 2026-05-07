package ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Atributos.estado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Atributos.subCategoria;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class bien {
    private String descripcion;
    private String fotoUrl;
    private LocalDate fechaVencimiento;
    private estado estado;
    private subCategoria subcategoria;

    // metodos
    public boolean estaVencido() {
        if (this.fechaVencimiento == null) return false;
        return this.fechaVencimiento.isBefore(LocalDate.now());
    }
}
