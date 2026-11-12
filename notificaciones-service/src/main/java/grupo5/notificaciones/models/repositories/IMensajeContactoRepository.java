package grupo5.notificaciones.models.repositories;

import grupo5.notificaciones.models.entities.contacto.EstadoMensajeContacto;
import grupo5.notificaciones.models.entities.contacto.MensajeContacto;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.lang.Nullable;

public interface IMensajeContactoRepository {

  /**
   * Guarda el mensaje y lo devuelve con su número. Una {@code claveEnvio} ya guardada da {@link
   * org.springframework.dao.DataIntegrityViolationException}.
   */
  MensajeContacto guardar(MensajeContacto mensaje);

  Optional<MensajeContacto> buscarPorNumero(long numero);

  Optional<MensajeContacto> buscarPorClaveEnvio(UUID claveEnvio);

  long contarPorCorreoDesde(String correo, LocalDateTime desde);

  long contarPorIpDesde(String ipOrigen, LocalDateTime desde);

  /** La página {@code pagina} (desde 0), del número más alto al más bajo. */
  List<MensajeContacto> listar(@Nullable EstadoMensajeContacto estado, int pagina, int tamanio);

  long contar(@Nullable EstadoMensajeContacto estado);
}
