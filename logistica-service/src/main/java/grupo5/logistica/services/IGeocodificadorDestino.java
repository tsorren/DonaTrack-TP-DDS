package grupo5.logistica.services;

import grupo5.logistica.models.entities.rutas.direccion.CoordenadasGeograficas;
import grupo5.logistica.models.entities.rutas.direccion.Direccion;
import java.util.Optional;

/**
 * Puerto de salida para geocodificación de domicilios de destino.
 *
 * <p>La integración con proveedor externo HTTP, timeouts, reintentos y manejo de fallos queda
 * diferida a E6. Cuando el adaptador no está configurado o un domicilio no puede geocodificarse con
 * precisión domiciliaria, debe retornar {@link Optional#empty()} sin inventar coordenadas ni usar
 * el centroide de la localidad.
 */
public interface IGeocodificadorDestino {

  Optional<CoordenadasGeograficas> geocodificar(Direccion destino);
}
