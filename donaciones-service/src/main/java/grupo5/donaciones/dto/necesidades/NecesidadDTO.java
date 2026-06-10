package grupo5.donaciones.dto.necesidades;

public record NecesidadDTO(
    Long id,
    Long entidadBeneficiariaId,
    String tipoNecesidad,
    String subcategoria,
    Integer cantidadNecesitada,
    String descripcion,
    String periodo) {}
