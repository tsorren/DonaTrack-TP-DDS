package grupo5.donaciones.seguridad;

import grupo5.common.seguridad.ReglasDeAcceso;
import java.util.List;
import org.springframework.stereotype.Component;

/** Rutas de Donaciones que no piden token (HB-08, D26). */
@Component
public class ReglasDeAccesoDonaciones implements ReglasDeAcceso {

  @Override
  public List<String> rutasPublicas() {
    return List.of("/api/entidades/publicas", "/api/necesidades/publicas");
  }
}
