package grupo5.notificaciones.infrastructure.persistencia.adapters;

import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.notificaciones.infrastructure.persistencia.entities.NotificacionEntity;
import grupo5.notificaciones.infrastructure.persistencia.mappers.NotificacionPersistenciaMapper;
import grupo5.notificaciones.infrastructure.persistencia.repositories.SpringDataNotificacionRepository;
import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.repositories.ConsultaBandeja;
import grupo5.notificaciones.models.repositories.INotificacionRepository;
import grupo5.notificaciones.models.repositories.PaginaBandeja;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("postgres")
public class NotificacionRepositoryJpaAdapter
    extends CrudRepositoryJpaAdapter<
        Notificacion, NotificacionEntity, SpringDataNotificacionRepository>
    implements INotificacionRepository {

  /** Las columnas de la bandeja: las del DTO, sin el historial de estados (spec R11-14, §3.8). */
  private static final String COLUMNAS =
      "SELECT id, persona_id, mensaje, fecha_creacion, estado_notificacion, leida, fecha_lectura,"
          + " tipo_evento, referencia_id ";

  private static final String DESDE =
      "FROM notificaciones.notificacion WHERE persona_id = :persona";

  private final NotificacionPersistenciaMapper mapper;
  private final EntityManager entityManager;

  public NotificacionRepositoryJpaAdapter(
      SpringDataNotificacionRepository springDataRepo,
      NotificacionPersistenciaMapper mapper,
      EntityManager entityManager) {
    super(springDataRepo, mapper::toEntity, mapper::toDomain);
    this.mapper = mapper;
    this.entityManager = entityManager;
  }

  @Override
  public List<Notificacion> findByEstado(EstadoNotificacion estado) {
    return springDataRepo.findByEstadoNotificacion(estado).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<Notificacion> findByPersonaId(UUID personaId) {
    return springDataRepo.findByPersonaId(personaId).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<Notificacion> findByPersonaIdAndLeida(UUID personaId, boolean leida) {
    return springDataRepo.findByPersonaIdAndLeida(personaId, leida).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public long countByPersonaIdAndLeidaFalse(UUID personaId) {
    return springDataRepo.countByPersonaIdAndLeidaFalse(personaId);
  }

  @Override
  public void marcarTodasComoLeidas(UUID personaId) {
    springDataRepo.marcarTodasComoLeidas(personaId);
  }

  /**
   * Dos consultas: la página (con el orden desempatado por id, para que una fila no salte de
   * página) y el total con los mismos filtros. Solo se agregan las condiciones de los filtros
   * presentes, así ningún parámetro llega nulo. La búsqueda iguala sin mayúsculas ni tildes con
   * {@code translate}, como {@link ConsultaBandeja#sinTildes(String)}.
   */
  @Override
  public PaginaBandeja bandeja(UUID personaId, ConsultaBandeja consulta) {
    List<String> condiciones = new ArrayList<>();
    Map<String, Object> parametros = new LinkedHashMap<>();
    parametros.put("persona", personaId);
    if (consulta.leida() != null) {
      condiciones.add("leida = :leida");
      parametros.put("leida", consulta.leida());
    }
    if (consulta.texto() != null) {
      condiciones.add(
          "translate(lower(mensaje), '"
              + ConsultaBandeja.CON_TILDE
              + "', '"
              + ConsultaBandeja.SIN_TILDE
              + "') LIKE :texto ESCAPE '\\'");
      parametros.put("texto", patron(consulta.texto()));
    }
    if (!consulta.tipos().isEmpty()) {
      condiciones.add("tipo_evento IN (:tipos)");
      parametros.put("tipos", List.copyOf(consulta.tipos()));
    }
    if (!consulta.excluirTipos().isEmpty()) {
      condiciones.add("(tipo_evento IS NULL OR tipo_evento NOT IN (:excluir))");
      parametros.put("excluir", List.copyOf(consulta.excluirTipos()));
    }
    String filtro = DESDE + condiciones.stream().map(c -> " AND " + c).reduce("", String::concat);
    String sentido = consulta.antiguas() ? "ASC" : "DESC";

    Query pagina =
        entityManager.createNativeQuery(
            COLUMNAS
                + filtro
                + " ORDER BY fecha_creacion "
                + sentido
                + ", id "
                + sentido
                + " LIMIT :limite OFFSET :desplazamiento");
    parametros.forEach(pagina::setParameter);
    pagina.setParameter("limite", consulta.tamanio());
    pagina.setParameter("desplazamiento", consulta.desplazamiento());
    List<?> filas = pagina.getResultList();
    List<Notificacion> notificaciones =
        filas.stream()
            .map(Object[].class::cast)
            .map(NotificacionRepositoryJpaAdapter::deFila)
            .toList();

    Query total = entityManager.createNativeQuery("SELECT count(*) " + filtro);
    parametros.forEach(total::setParameter);
    return new PaginaBandeja(notificaciones, ((Number) total.getSingleResult()).longValue());
  }

  /** "%texto%", con {@code %}, {@code _} y {@code \} escapados. */
  static String patron(String texto) {
    return "%" + texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
  }

  private static Notificacion deFila(Object[] fila) {
    Notificacion notificacion =
        new Notificacion(
            uuid(fila[0]),
            uuid(fila[1]),
            (String) fila[2],
            momento(fila[3]),
            EstadoNotificacion.valueOf(fila[4].toString()),
            List.of(),
            Boolean.TRUE.equals(fila[5]),
            momento(fila[6]));
    notificacion.etiquetar((String) fila[7], uuid(fila[8]));
    return notificacion;
  }

  private static UUID uuid(Object valor) {
    if (valor == null) {
      return null;
    }
    return valor instanceof UUID id ? id : UUID.fromString(valor.toString());
  }

  private static LocalDateTime momento(Object valor) {
    if (valor instanceof Timestamp marca) {
      return marca.toLocalDateTime();
    }
    return (LocalDateTime) valor;
  }
}
