package grupo5.logistica.dto.rutas;

public record DireccionDTO(
    String calle,
    Integer altura,
    Integer piso,
    String departamento,
    String codigoPostal,
    String localidad,
    String provincia,
    String pais) {}
