package grupo5.donaciones.models.entities.propuestas;

import grupo5.common.events.EventoDeDominio;
import java.util.List;
import java.util.UUID;
import lombok.Getter;

@Getter
public class PropuestaAprobada extends EventoDeDominio {
  private final UUID propuestaId;
  private final UUID necesidadId;
  private final List<PosibleFragmentacion> fragmentaciones;
  private final String actor;

  public PropuestaAprobada(
      UUID propuestaId,
      UUID necesidadId,
      List<PosibleFragmentacion> fragmentaciones,
      String actor) {
    super();
    this.propuestaId = propuestaId;
    this.necesidadId = necesidadId;
    this.fragmentaciones = fragmentaciones != null ? List.copyOf(fragmentaciones) : List.of();
    this.actor = actor;
  }

  public UUID propuestaId() {
    return propuestaId;
  }

  public UUID necesidadId() {
    return necesidadId;
  }

  public List<PosibleFragmentacion> fragmentaciones() {
    return fragmentaciones;
  }

  public String actor() {
    return actor;
  }
}
