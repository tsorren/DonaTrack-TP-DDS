package grupo5.tests.dto;

public record DireccionTestDTO(
    String calle,
    Integer altura,
    Integer piso,
    String departamento,
    String codigoPostal,
    String localidad,
    String provincia,
    String pais) {
  public static DireccionTestDTO defaultMedrano() {
    return new DireccionTestDTO(
        "Av. Medrano", 951, null, null, "1179", "CABA", "CABA", "Argentina");
  }
}
