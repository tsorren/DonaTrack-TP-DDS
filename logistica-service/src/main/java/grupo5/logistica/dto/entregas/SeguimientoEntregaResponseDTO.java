package grupo5.logistica.dto.entregas;

import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    LocalDateTime horaSalida,
    LocalDateTime horaArribo,
    LocalDateTime ultimaActualizacion,
    List<ParadaSeguimientoDTO> paradas) {}
