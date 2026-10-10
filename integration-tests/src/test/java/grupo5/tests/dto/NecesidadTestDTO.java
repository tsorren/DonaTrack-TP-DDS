package grupo5.tests.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NecesidadTestDTO(
    String tipo,
    UUID idEntidad,
    UUID idSubcategoria,
    Integer cantidadNecesitada,
    String descripcion,
    String fechaInicio,
    String fechaFin) {}
