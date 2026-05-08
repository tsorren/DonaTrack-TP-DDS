package ar.edu.utn.frba.ddsi.donaciones.models.entities.bienes;

import lombok.Data;

import java.time.LocalDate;


@Data
public class Bien {
  private String descripcion;
  private String fotoUrl;
  private LocalDate fechaVencimiento;
  private Estado estado;
  private SubCategoria subcategoria;

  // metodos
  public boolean estaVencido() {
    if (this.fechaVencimiento == null) return false;
    return this.fechaVencimiento.isBefore(LocalDate.now());
  }
}
