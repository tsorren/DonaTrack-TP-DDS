package grupo5.donaciones.dto.comunicaciones;

import java.time.LocalDateTime;
import java.util.UUID;

/** Evento de vuelta {@code entrega.en-puerta}: el camión llegó a la entidad (HB-22). */
public record EventoEntregaEnPuerta(
    UUID entregaId, UUID donacionIndependienteId, LocalDateTime fechaLlegada) {}
