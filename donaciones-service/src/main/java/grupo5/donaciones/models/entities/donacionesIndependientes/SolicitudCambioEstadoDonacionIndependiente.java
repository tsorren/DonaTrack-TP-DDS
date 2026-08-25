package grupo5.donaciones.models.entities.donacionesIndependientes;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import lombok.Getter;

@Getter
public class SolicitudCambioEstadoDonacionIndependiente {
  private final TipoEstadoDonacion estado;
  private final Necesidad necesidad;
  private final String justificacion;
  private final String urlMapa;
  private final String patenteCamion;
  private final Boolean replanificable;
  private final String actor;

  public SolicitudCambioEstadoDonacionIndependiente(
      TipoEstadoDonacion estado,
      Necesidad necesidad,
      String justificacion,
      String urlMapa,
      String patenteCamion,
      Boolean replanificable,
      String actor) {
    if (estado == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    this.estado = estado;
    this.necesidad = necesidad;
    this.justificacion = justificacion;
    this.urlMapa = urlMapa;
    this.patenteCamion = patenteCamion;
    this.replanificable = replanificable;
    this.actor = (actor != null && !actor.isBlank()) ? actor : "SISTEMA";
  }

  public SolicitudCambioEstadoDonacionIndependiente(
      TipoEstadoDonacion estado, Necesidad necesidad, String actor) {
    this(estado, necesidad, null, null, null, null, actor);
  }

  public SolicitudCambioEstadoDonacionIndependiente(TipoEstadoDonacion estado, String actor) {
    this(estado, null, null, null, null, null, actor);
  }
}
