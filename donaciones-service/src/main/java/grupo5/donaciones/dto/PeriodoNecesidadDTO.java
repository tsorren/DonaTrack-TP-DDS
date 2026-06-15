package grupo5.donaciones.dto;

import grupo5.common.repositories.RecursoDTO;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;

@Data
public class PeriodoNecesidadDTO implements RecursoDTO {
  private UUID id;
  private UUID necesidadId; // Enlace con la Necesidad Recurrente padre
  private LocalDate fechaInicio;
  private LocalDate fechaFin;
  private boolean estaSatisfecho;
}
