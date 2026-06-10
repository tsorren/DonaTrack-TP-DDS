package grupo5.donaciones.dto.necesidades;

public record CrearNecesidadDTO(
    Long entidadBeneficiariaId,
    String tipoNecesidad,
    String subcategoria,
    Integer cantidadNecesitada,
    String descripcion,
    String periodo) {}
