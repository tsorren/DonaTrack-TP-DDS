package grupo5.cliente.puertos;

import grupo5.cliente.vistas.RecibidasVista;
import java.util.UUID;

/** Las donaciones asignadas a una entidad, para Donaciones recibidas (RF-11). */
public interface RecibidasPuerto {

  RecibidasVista recibidas(UUID entidad);
}
