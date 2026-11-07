package grupo5.logistica.dto.entregas;

import com.fasterxml.jackson.annotation.JsonFormat;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Proyección de seguimiento de una entrega y el progreso de su ruta planificada sin GPS en tiempo
 * real (RF-09 y RF-14).
 *
 * <p>Distingue explícitamente {@code paradasEntregadas} (únicamente en estado {@code ENTREGADA}) de
 * {@code paradasProcesadas} (suma de {@code ENTREGADA} + {@code NO_RECIBIDA}). El campo {@code
 * ultimaActualizacion} se calcula exclusivamente a partir de marcas de tiempo reales persistidas
 * (siendo {@code null} cuando ninguna existe).
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
    List<ParadaSeguimientoDTO> paradas) {}
