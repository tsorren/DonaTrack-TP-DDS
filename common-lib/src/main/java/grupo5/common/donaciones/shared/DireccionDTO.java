package grupo5.common.donaciones.shared;

public record DireccionDTO(
    String calle,
    Integer altura,
    Integer piso,
    String departamento,
    String codigoPostal,
    LocalidadDTO localidad) {}
