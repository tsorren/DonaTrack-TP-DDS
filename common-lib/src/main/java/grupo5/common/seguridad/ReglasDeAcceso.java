package grupo5.common.seguridad;

import java.util.List;

/** Lo que cada servicio aporta a la cadena de seguridad: las rutas que no piden token. */
public interface ReglasDeAcceso {
  List<String> rutasPublicas();
}
