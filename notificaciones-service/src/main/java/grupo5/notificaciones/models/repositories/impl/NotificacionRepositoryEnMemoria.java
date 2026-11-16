package grupo5.notificaciones.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.repositories.ConsultaBandeja;
import grupo5.notificaciones.models.repositories.INotificacionRepository;
import grupo5.notificaciones.models.repositories.PaginaBandeja;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!postgres")
public class NotificacionRepositoryEnMemoria extends CrudRepositoryEnMemoria<Notificacion>
    implements INotificacionRepository {

  @Override
  public List<Notificacion> findByEstado(EstadoNotificacion estado) {
    return this.findAll().stream().filter(n -> n.getEstadoNotificacion() == estado).toList();
  }

  @Override
  public List<Notificacion> findByPersonaId(UUID personaId) {
    return this.findAll().stream()
        .filter(n -> n.getPersonaId() != null && n.getPersonaId().equals(personaId))
        .toList();
  }

  @Override
  public List<Notificacion> findByPersonaIdAndLeida(UUID personaId, boolean leida) {
    return this.findAll().stream()
        .filter(
            n ->
                n.getPersonaId() != null
                    && n.getPersonaId().equals(personaId)
                    && n.isLeida() == leida)
        .toList();
  }

  @Override
  public long countByPersonaIdAndLeidaFalse(UUID personaId) {
    return this.findAll().stream()
        .filter(n -> n.getPersonaId() != null && n.getPersonaId().equals(personaId) && !n.isLeida())
        .count();
  }

  @Override
  public void marcarTodasComoLeidas(UUID personaId) {
    this.findAll().stream()
        .filter(n -> n.getPersonaId() != null && n.getPersonaId().equals(personaId) && !n.isLeida())
        .forEach(Notificacion::marcarComoLeida);
  }

  @Override
  public PaginaBandeja bandeja(UUID personaId, ConsultaBandeja consulta) {
    Comparator<Notificacion> orden =
        Comparator.comparing(Notificacion::getFechaCreacion)
            .thenComparing(n -> n.getId().toString());
    List<Notificacion> cumplen =
        findByPersonaId(personaId).stream()
            .filter(n -> cumple(n, consulta))
            .sorted(consulta.antiguas() ? orden : orden.reversed())
            .toList();
    List<Notificacion> pagina =
        cumplen.stream().skip(consulta.desplazamiento()).limit(consulta.tamanio()).toList();
    return new PaginaBandeja(pagina, cumplen.size());
  }

  private static boolean cumple(Notificacion n, ConsultaBandeja consulta) {
    return (consulta.leida() == null || n.isLeida() == consulta.leida())
        && contieneTexto(n, consulta.texto())
        && esDeTipo(n.getTipoEvento(), consulta);
  }

  private static boolean contieneTexto(Notificacion n, String texto) {
    if (texto == null) {
      return true;
    }
    return n.getMensaje() != null && ConsultaBandeja.sinTildes(n.getMensaje()).contains(texto);
  }

  /** Sin tipo no entra en {@code tipos}, pero sí en "todas menos {@code excluirTipos}". */
  private static boolean esDeTipo(String tipo, ConsultaBandeja consulta) {
    if (!consulta.tipos().isEmpty() && (tipo == null || !consulta.tipos().contains(tipo))) {
      return false;
    }
    return tipo == null || !consulta.excluirTipos().contains(tipo);
  }
}
