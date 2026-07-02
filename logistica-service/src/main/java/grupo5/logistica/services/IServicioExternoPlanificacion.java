package grupo5.logistica.services;

import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.rutas.Ruta;
import java.util.List;

/**
 * Abstrae al componente encargado de calcular la planificación de rutas para un lote de entregas.
 * El {@code PlanificadorDeEntregas} depende únicamente de esta interfaz, lo que permite
 * intercambiar la implementación sin modificar la orquestación asincrónica: hoy la satisface {@code
 * GeneradorDeRutas} (algoritmo nativo, sin dependencias externas), y en el futuro podría
 * satisfacerla un adaptador hacia un proveedor externo real (ver {@code logistica.ruteador.url})
 * sin romper este contrato.
 */
public interface IServicioExternoPlanificacion {

  /**
   * Genera las rutas de reparto para un lote de entregas, distribuyéndolas entre los camiones
   * operativos disponibles.
   *
   * @param entregas entregas en estado pendiente de ruta (máximo 100 por restricción de negocio).
   * @param camiones camiones disponibles para la jornada.
   * @return una ruta por cada camión que recibió al menos una entrega.
   */
  List<Ruta> generarRutas(List<Entrega> entregas, List<Camion> camiones);
}
