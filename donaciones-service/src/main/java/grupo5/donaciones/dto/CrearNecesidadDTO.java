package grupo5.donaciones.dto;

import java.time.Period;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrearNecesidadDTO {
  private String tipo; // "RECURRENTE" o "EXTRAORDINARIA"
  private Long entidadId;
  private Long subcategoriaId;
  private Integer cantidadNecesitada;
  private String descripcion;

  // Exclusivo para "Recurrente"
  private Period frecuencia;
}
