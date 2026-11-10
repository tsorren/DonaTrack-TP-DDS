package grupo5.donaciones.dto.donacionesIndependientes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record DonacionIndependienteResponseDTO(
    UUID id,
    UUID donacionOriginalId,
    String descripcion,
    String estadoActual,
    LocalDateTime fechaRegistro,
    List<CambioEstadoDIResponseDTO> historial,
    List<ItemDonacionIndependienteResponseDTO> items,
    int cantidad,
    UUID entidadBeneficiariaId,
    UUID necesidadId) {

  /** Sin necesidad: la donación no está asignada a una (o la respuesta es anterior a HB-17). */
  @SuppressWarnings("java:S107")
  public DonacionIndependienteResponseDTO(
      UUID id,
      UUID donacionOriginalId,
      String descripcion,
      String estadoActual,
      LocalDateTime fechaRegistro,
      List<CambioEstadoDIResponseDTO> historial,
      List<ItemDonacionIndependienteResponseDTO> items,
      int cantidad,
      UUID entidadBeneficiariaId) {
    this(
        id,
        donacionOriginalId,
        descripcion,
        estadoActual,
        fechaRegistro,
        historial,
        items,
        cantidad,
        entidadBeneficiariaId,
        null);
  }
}
