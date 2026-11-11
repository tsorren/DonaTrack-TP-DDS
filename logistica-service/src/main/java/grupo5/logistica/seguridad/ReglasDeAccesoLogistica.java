package grupo5.logistica.seguridad;

import grupo5.common.seguridad.ReglasDeAcceso;
import java.util.List;
import org.springframework.stereotype.Component;

/** Rutas de Logística que no piden token: el mapa de entregas de la portada (RF-02). */
@Component
public class ReglasDeAccesoLogistica implements ReglasDeAcceso {

  @Override
  public List<String> rutasPublicas() {
    return List.of("/api/entregas/mapa");
  }
}
