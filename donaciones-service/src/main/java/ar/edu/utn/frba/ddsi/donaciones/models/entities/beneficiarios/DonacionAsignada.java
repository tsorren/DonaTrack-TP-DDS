package ar.edu.utn.frba.ddsi.donaciones.models.entities.beneficiarios;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.bienes.Bien;
import java.time.LocalDate;
import lombok.Data;

@Data
public class DonacionAsignada {
  private Bien bien;
  private Integer cantidad;
  private LocalDate fechaAsignacion;
}
