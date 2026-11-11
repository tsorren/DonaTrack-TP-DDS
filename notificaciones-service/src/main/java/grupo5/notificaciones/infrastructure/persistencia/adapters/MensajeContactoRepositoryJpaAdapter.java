package grupo5.notificaciones.infrastructure.persistencia.adapters;

import grupo5.notificaciones.infrastructure.persistencia.entities.MensajeContactoEntity;
import grupo5.notificaciones.infrastructure.persistencia.repositories.SpringDataMensajeContactoRepository;
import grupo5.notificaciones.models.entities.contacto.EstadoMensajeContacto;
import grupo5.notificaciones.models.entities.contacto.MensajeContacto;
import grupo5.notificaciones.models.repositories.IMensajeContactoRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

@Repository
@Profile("postgres")
public class MensajeContactoRepositoryJpaAdapter implements IMensajeContactoRepository {

  private final SpringDataMensajeContactoRepository springDataRepo;

  public MensajeContactoRepositoryJpaAdapter(SpringDataMensajeContactoRepository springDataRepo) {
    this.springDataRepo = springDataRepo;
  }

  /** {@code saveAndFlush}: la clave repetida falla acá y no al cerrar la transacción. */
  @Override
  public MensajeContacto guardar(MensajeContacto mensaje) {
    return toDomain(springDataRepo.saveAndFlush(toEntity(mensaje)));
  }

  @Override
  public Optional<MensajeContacto> buscarPorNumero(long numero) {
    return springDataRepo.findById(numero).map(MensajeContactoRepositoryJpaAdapter::toDomain);
  }

  @Override
  public Optional<MensajeContacto> buscarPorClaveEnvio(UUID claveEnvio) {
    return springDataRepo
        .findByClaveEnvio(claveEnvio)
        .map(MensajeContactoRepositoryJpaAdapter::toDomain);
  }

  @Override
  public long contarPorCorreoDesde(String correo, LocalDateTime desde) {
    return springDataRepo.countByCorreoAndRecibidoEnGreaterThanEqual(correo, desde);
  }

  @Override
  public long contarPorIpDesde(String ipOrigen, LocalDateTime desde) {
    return springDataRepo.countByIpOrigenAndRecibidoEnGreaterThanEqual(ipOrigen, desde);
  }

  @Override
  public List<MensajeContacto> listar(
      @Nullable EstadoMensajeContacto estado, int pagina, int tamanio) {
    PageRequest pedido = PageRequest.of(pagina, tamanio, Sort.by(Sort.Direction.DESC, "numero"));
    List<MensajeContactoEntity> entidades =
        estado == null
            ? springDataRepo.findAll(pedido).getContent()
            : springDataRepo.findByEstado(estado, pedido);
    return entidades.stream().map(MensajeContactoRepositoryJpaAdapter::toDomain).toList();
  }

  @Override
  public long contar(@Nullable EstadoMensajeContacto estado) {
    return estado == null ? springDataRepo.count() : springDataRepo.countByEstado(estado);
  }

  private static MensajeContactoEntity toEntity(MensajeContacto m) {
    MensajeContactoEntity e = new MensajeContactoEntity();
    e.setNumero(m.getNumero());
    e.setClaveEnvio(m.getClaveEnvio());
    e.setTema(m.getTema());
    e.setTexto(m.getTexto());
    e.setCodigoError(m.getCodigoError());
    e.setCorreo(m.getCorreo());
    e.setIpOrigen(m.getIpOrigen());
    e.setEstado(m.getEstado());
    e.setRecibidoEn(m.getRecibidoEn());
    e.setRespuesta(m.getRespuesta());
    e.setRespondidoEn(m.getRespondidoEn());
    return e;
  }

  private static MensajeContacto toDomain(MensajeContactoEntity e) {
    return new MensajeContacto(
        e.getNumero(),
        e.getClaveEnvio(),
        e.getTema(),
        e.getTexto(),
        e.getCodigoError(),
        e.getCorreo(),
        e.getIpOrigen(),
        e.getRecibidoEn(),
        e.getEstado(),
        e.getRespuesta(),
        e.getRespondidoEn());
  }
}
