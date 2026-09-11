package grupo5.logistica.models.entities.rutas.eventos;

import java.util.UUID;
import lombok.Getter;

/** Evento de dominio emitido cuando una entrega queda asignada a una ruta. */
@Getter
public final class EventoRutaAsignada extends EventoRuta {

  private final UUID rutaId;
  private final UUID entregaId;

  public EventoRutaAsignada(UUID rutaId, UUID entregaId) {
    this.rutaId = rutaId;
    this.entregaId = entregaId;
  }
}
