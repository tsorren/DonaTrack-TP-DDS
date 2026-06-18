package grupo5.donaciones.dto.direcciones;

public record DireccionOutputDTO(
    String calle,
    Integer altura,
    Integer piso,
    String departamento,
    String codigoPostal,
    String localidad,
    String provincia,
    String pais) {}
