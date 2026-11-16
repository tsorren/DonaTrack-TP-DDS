package grupo5.notificaciones.models.repositories.impl;

import grupo5.notificaciones.models.entities.contacto.EstadoMensajeContacto;
import grupo5.notificaciones.models.entities.contacto.MensajeContacto;
import grupo5.notificaciones.models.repositories.IMensajeContactoRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!postgres")
public class MensajeContactoRepositoryEnMemoria implements IMensajeContactoRepository {

  private final Map<Long, MensajeContacto> mensajes = new ConcurrentHashMap<>();
  private final AtomicLong secuencia = new AtomicLong();

  @Override
  public synchronized MensajeContacto guardar(MensajeContacto mensaje) {
    if (mensaje.getNumero() == null) {
      if (buscarPorClaveEnvio(mensaje.getClaveEnvio()).isPresent()) {
        throw new DataIntegrityViolationException("claveEnvio ya guardada");
      }
      mensaje.asignarNumero(secuencia.incrementAndGet());
    }
    mensajes.put(mensaje.getNumero(), mensaje);
    return mensaje;
  }

  @Override
  public Optional<MensajeContacto> buscarPorNumero(long numero) {
    return Optional.ofNullable(mensajes.get(numero));
  }

  @Override
  public Optional<MensajeContacto> buscarPorClaveEnvio(UUID claveEnvio) {
    return mensajes.values().stream().filter(m -> m.getClaveEnvio().equals(claveEnvio)).findFirst();
  }

  @Override
  public long contarPorCorreoDesde(String correo, LocalDateTime desde) {
    return mensajes.values().stream()
        .filter(m -> m.getCorreo().equals(correo) && !m.getRecibidoEn().isBefore(desde))
        .count();
  }

  @Override
  public long contarPorIpDesde(String ipOrigen, LocalDateTime desde) {
    return mensajes.values().stream()
        .filter(m -> ipOrigen.equals(m.getIpOrigen()) && !m.getRecibidoEn().isBefore(desde))
        .count();
  }

  @Override
  public List<MensajeContacto> listar(
      @Nullable EstadoMensajeContacto estado, int pagina, int tamanio) {
    return conEstado(estado)
        .sorted(Comparator.comparing(MensajeContacto::getNumero).reversed())
        .skip((long) pagina * tamanio)
        .limit(tamanio)
        .toList();
  }

  @Override
  public long contar(@Nullable EstadoMensajeContacto estado) {
    return conEstado(estado).count();
  }

  private Stream<MensajeContacto> conEstado(@Nullable EstadoMensajeContacto estado) {
    return mensajes.values().stream().filter(m -> estado == null || m.getEstado() == estado);
  }
}
