package grupo5.cliente.puertos;

import grupo5.cliente.vistas.MisDonacionesVista;
import java.util.UUID;

/** Las donaciones de un donante, para Mis donaciones. */
public interface DonacionesPuerto {

  MisDonacionesVista misDonaciones(UUID donante);
}
