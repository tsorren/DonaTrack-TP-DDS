package grupo5.donaciones.dto;

import grupo5.common.repositories.RecursoDTO;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;

@Data
public class NecesidadDTO implements RecursoDTO {
  private UUID id;
  private String tipo; // recurrente o extraordinaria
  private UUID entidadId;
  private String subcategoriaNombre;
  private Integer cantidadNecesitada;
  private String descripcion;
  private Boolean estaSatisfecha;
  private LocalDate fechaInicio;
  private LocalDate fechaFin;
}
