package ar.edu.utn.frba.ddsi.donaciones.models.entities.beneficiarios;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.bienes.Bien;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DonacionAsignada {
  private Bien bien;
  private Integer cantidad;
  private LocalDate fechaAsignacion;
}
