package grupo5.tests.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ItemDonacionTestDTO(
    String descripcionBien,
    String fotoUrl,
    String fechaVencimiento,
    String estadoBien,
    Double pesoUnitario,
    Double volumenUnitario,
    Integer cantidad) {
  public static ItemDonacionTestDTO simple(String descripcionBien, int cantidad) {
    return new ItemDonacionTestDTO(
        descripcionBien, null, "2027-12-31", "NUEVO", 1.0, 0.01, cantidad);
  }
}
