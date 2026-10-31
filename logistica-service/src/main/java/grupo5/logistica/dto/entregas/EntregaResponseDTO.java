package grupo5.logistica.dto.entregas;

import com.fasterxml.jackson.annotation.JsonFormat;
import grupo5.logistica.dto.rutas.DireccionDTO;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EntregaResponseDTO(
    UUID id,
    UUID idRuta,
    UUID idDonacion,
    UUID idBeneficiaria,
    DireccionDTO destino,
    EstadoEntrega estadoActual,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant horaSalida,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant horaArribo,
    String fotoRecepcionUrl,
    List<String> fotosRecepcion,
    float pesoTotalKG,
    float volumenTotalM3,
    List<CambioEstadoEntregaResponseDTO> historialEstado) {

  @SuppressWarnings("java:S107")
  public EntregaResponseDTO(
      UUID id,
      UUID idRuta,
      UUID idDonacion,
      UUID idBeneficiaria,
      DireccionDTO destino,
      EstadoEntrega estadoActual,
      Instant horaSalida,
      Instant horaArribo,
      String fotoRecepcionUrl,
      float pesoTotalKG,
      float volumenTotalM3,
      List<CambioEstadoEntregaResponseDTO> historialEstado) {
    this(
        id,
        idRuta,
        idDonacion,
        idBeneficiaria,
        destino,
        estadoActual,
        horaSalida,
        horaArribo,
        fotoRecepcionUrl,
        fotoRecepcionUrl != null && !fotoRecepcionUrl.isBlank()
            ? List.of(fotoRecepcionUrl)
            : List.of(),
        pesoTotalKG,
        volumenTotalM3,
        historialEstado);
  }
}
