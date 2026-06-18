package grupo5.donaciones.dto;

import grupo5.common.repositories.RecursoDTO;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;

@Data
public class NecesidadDTO implements RecursoDTO {
  private UUID id;
  private String tipo; // recurrente o extraordinaria
  private UUID idEntidad;
  private UUID idSubcategoria; // TODO: Reemplazar por UUID idSubcategoria, usar repository
  private Integer cantidadNecesitada;
  private String descripcion;
  private Boolean estaSatisfecha;
  private LocalDate fechaInicio;
  private LocalDate fechaFin;

  public NecesidadDTO(
      UUID id,
      String tipo,
      UUID idEntidad,
      UUID idSubcategoria,
      Integer cantidadNecesitada,
      String descripcion,
      Boolean estaSatisfecha,
      LocalDate fechaInicio,
      LocalDate fechaFin) {
    this.id = id;
    this.tipo = tipo;
    this.idEntidad = idEntidad;
    this.idSubcategoria = idSubcategoria;
    this.cantidadNecesitada = cantidadNecesitada;
    this.descripcion = descripcion;
    this.estaSatisfecha = estaSatisfecha;
    this.fechaInicio = fechaInicio;
    this.fechaFin = fechaFin;
  }
}
