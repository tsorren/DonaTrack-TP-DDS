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
    boolean cancelacionPedida) {

  /** Sin la marca de HB-19 (la necesidad no se dio de baja). */
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
        false);
  }
}
