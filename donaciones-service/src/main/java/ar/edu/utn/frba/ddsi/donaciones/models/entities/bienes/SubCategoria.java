package ar.edu.utn.frba.ddsi.donaciones.models.entities.bienes;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.DonacionIndependiente;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class SubCategoria {
  private Categoria categoria;
  private String nombre;

  private final List<DonacionIndependiente> donaciones = new ArrayList<>();

  public void agregarDonacion(DonacionIndependiente donacion) {
    if (donacion != null) {
      this.donaciones.add(donacion);
    }
  }

  public Integer calcularStock() {
    return this.donaciones.stream().mapToInt(DonacionIndependiente::getCantidad).sum();
  }
}
