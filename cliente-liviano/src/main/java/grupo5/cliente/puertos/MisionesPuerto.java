package grupo5.cliente.puertos;

import grupo5.cliente.vistas.MisionesVista;
import java.util.UUID;

/** Puerto para obtener las misiones e insignias del donante (RF-07). */
public interface MisionesPuerto {

  MisionesVista misiones(UUID donanteId);
}
