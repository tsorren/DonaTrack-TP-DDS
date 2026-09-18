package grupo5.logistica.models.entities.rutas.eventos;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Getter;

/** Evento de dominio emitido cuando una ruta comienza su traslado. */
@Getter
public final class EventoRutaIniciada extends EventoRuta {

  private final UUID rutaId;
  private final UUID camionId;
  private final List<UUID> entregaIds;
  private final LocalDateTime fechaInicio;

  public EventoRutaIniciada(
      UUID rutaId, UUID camionId, List<UUID> entregaIds, LocalDateTime fechaInicio) {
    this.rutaId = rutaId;
    this.camionId = camionId;
    this.entregaIds = List.copyOf(entregaIds);
    this.fechaInicio = fechaInicio;
  }
}
