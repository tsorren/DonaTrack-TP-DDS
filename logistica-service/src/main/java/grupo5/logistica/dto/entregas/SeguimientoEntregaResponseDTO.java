package grupo5.logistica.dto.entregas;

import com.fasterxml.jackson.annotation.JsonFormat;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Proyección de seguimiento de una entrega y el progreso de su ruta planificada (RF-09 y RF-14).
 *
 * <p>Distingue explícitamente {@code paradasEntregadas} (únicamente en estado {@code ENTREGADA}) de
 * {@code paradasProcesadas} (suma de {@code ENTREGADA} + {@code NO_RECIBIDA}). El campo {@code
 * ultimaActualizacion} se calcula exclusivamente a partir de marcas de tiempo reales persistidas
 * (siendo {@code null} cuando ninguna existe).
 *
 * <p>{@code posicionCamion} es la última posición conocida del camión, exacta (ADR-E6-07); es
 * {@code null} cuando la ruta no está {@code EN_TRASLADO} o la fuente todavía no informó ninguna.
 */
public record SeguimientoEntregaResponseDTO(
    UUID idEntrega,
    UUID idDonacion,
    EstadoEntrega estadoEntrega,
    boolean rutaAsignada,
    UUID idRuta,
    LocalDate fechaRuta,
    EstadoRuta estadoRuta,
    String patenteCamion,
    Integer ordenVisita,
    int totalParadas,
    int paradasEntregadas,
    int paradasProcesadas,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant horaSalida,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant horaArribo,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant ultimaActualizacion,
    List<ParadaSeguimientoDTO> paradas,
    PosicionCamionDTO posicionCamion) {

  /** Seguimiento sin posición del camión. */
  @SuppressWarnings("java:S107")
  public SeguimientoEntregaResponseDTO(
      UUID idEntrega,
      UUID idDonacion,
      EstadoEntrega estadoEntrega,
      boolean rutaAsignada,
      UUID idRuta,
      LocalDate fechaRuta,
      EstadoRuta estadoRuta,
      String patenteCamion,
      Integer ordenVisita,
      int totalParadas,
      int paradasEntregadas,
      int paradasProcesadas,
      Instant horaSalida,
      Instant horaArribo,
      Instant ultimaActualizacion,
      List<ParadaSeguimientoDTO> paradas) {
    this(
        idEntrega,
        idDonacion,
        estadoEntrega,
        rutaAsignada,
        idRuta,
        fechaRuta,
        estadoRuta,
        patenteCamion,
        ordenVisita,
        totalParadas,
        paradasEntregadas,
        paradasProcesadas,
        horaSalida,
        horaArribo,
        ultimaActualizacion,
        paradas,
        null);
  }
}
