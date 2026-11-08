package grupo5.donaciones.seguridad;

import grupo5.donaciones.services.IDonacionesIndependientesService;
import grupo5.donaciones.services.IDonacionesService;
import grupo5.donaciones.services.INecesidadesService;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Dueño de cada recurso, para {@code principal.puedeOperarSobre(@duenioDe.necesidad(#id))}. */
@Component("duenioDe")
public class DuenioDe {

  private final IDonacionesService donaciones;
  private final IDonacionesIndependientesService independientes;
  private final INecesidadesService necesidades;

  public DuenioDe(
      IDonacionesService donaciones,
      IDonacionesIndependientesService independientes,
      INecesidadesService necesidades) {
    this.donaciones = donaciones;
    this.independientes = independientes;
    this.necesidades = necesidades;
  }

  public UUID donacion(UUID id) {
    return donaciones.obtenerDonacion(id).donante().idDonante();
  }

  public UUID donacionIndependiente(UUID id) {
    return donacion(independientes.obtener(id).donacionOriginalId());
  }

  public UUID necesidad(UUID id) {
    return necesidades.obtenerPorId(id).idEntidad();
  }
}
