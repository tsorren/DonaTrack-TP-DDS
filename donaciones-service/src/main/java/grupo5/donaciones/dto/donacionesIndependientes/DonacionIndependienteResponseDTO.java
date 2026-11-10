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
    UUID necesidadId,
    boolean cancelacionPedida,
    QuienDonoDTO quienDono) {

  /** Sin quién donó (respuesta anterior a HB-20). */
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
      UUID entidadBeneficiariaId,
      UUID necesidadId,
      boolean cancelacionPedida) {
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
        necesidadId,
        cancelacionPedida,
        null);
  }

  /** Sin necesidad (o respuesta anterior a HB-17) y sin la marca de HB-19. */
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
        null,
        false);
  }
}
