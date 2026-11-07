package grupo5.incentivos.models.repositories;

import grupo5.incentivos.models.entities.donante.eventos.EventoDonanteIncentivos;
import grupo5.incentivos.models.outbox.EntradaOutbox;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Outbox de los avisos que Incentivos publica en RabbitMQ (Transactional Outbox): los avisos se
 * guardan en la base junto con el cambio que los origina y un relay los publica después.
 */
public interface IOutboxIncentivos {

  /**
   * Registra los avisos de los eventos de dominio de un donante. Participa de la transacción que
   * esté abierta: se llama junto con el guardado del donante para que queden o no queden juntos.
   * Los eventos que no generan aviso se ignoran.
   */
  void registrarAvisos(List<EventoDonanteIncentivos> eventos);

  /** Registra el aviso de un donante inactivo. No va atado a un cambio del donante. */
  void registrarInactividad(UUID idPersona, int diasInactivo);

  /**
   * Toma hasta {@code limite} entradas pendientes cuyo próximo intento ya venció y las reserva por
   * {@code arrendamiento}: mientras dure, ninguna otra réplica las toma. Si quien las tomó se cae,
   * vuelven a estar disponibles al vencer el arrendamiento.
   */
  List<EntradaOutbox> reclamarPendientes(LocalDateTime ahora, Duration arrendamiento, int limite);

  /** Guarda el estado, los intentos y el próximo intento de la entrada. */
  void actualizar(EntradaOutbox entrada);
}
