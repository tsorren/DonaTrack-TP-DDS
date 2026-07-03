package grupo5.logistica.services.impl;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.services.AlgoritmoAsignadorDeEntregas;
import grupo5.logistica.services.AlgoritmoOrdenadorDeEntregas;
import grupo5.logistica.services.IServicioExternoPlanificacion;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Componente coordinador encargado de generar las hojas de ruta de reparto de forma nativa, sin
 * depender de servicios externos. Recibe las entregas recién creadas (post-asignación de la
 * donación a una entidad beneficiaria) junto con los camiones operativos, y devuelve, por cada
 * camión, la ruta con la lista ordenada de entregas que debe realizar.
 *
 * <p>Aplica el patrón <b>Strategy</b> delegando en dos abstracciones intercambiables:
 *
 * <ul>
 *   <li>{@link AlgoritmoOrdenadorDeEntregas}: decide el orden de las entregas.
 *   <li>{@link AlgoritmoAsignadorDeEntregas}: decide en qué camión entra cada entrega.
 * </ul>
 *
 * <p>Al no tener dependencias de infraestructura (repositorios, clientes HTTP, etc.), esta clase es
 * puramente de dominio/orquestación de estrategias y resulta sencilla de testear. También
 * implementa {@link IServicioExternoPlanificacion} para que el {@code PlanificadorDeEntregas} pueda
 * utilizarla como su proveedor de planificación de rutas por defecto.
 */
@Component
public class GeneradorDeRutas implements IServicioExternoPlanificacion {

  private final AlgoritmoOrdenadorDeEntregas ordenadorEntregas;
  private final AlgoritmoAsignadorDeEntregas asignadorDeEntregas;

  public GeneradorDeRutas(
      AlgoritmoOrdenadorDeEntregas ordenadorEntregas,
      AlgoritmoAsignadorDeEntregas asignadorDeEntregas) {
    if (ordenadorEntregas == null || asignadorDeEntregas == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    this.ordenadorEntregas = ordenadorEntregas;
    this.asignadorDeEntregas = asignadorDeEntregas;
  }

  /**
   * Genera las rutas de reparto del día siguiente para la flota.
   *
   * @param entregas entregas en estado pendiente de ruta (recibidas del proceso de asignación de
   *     donaciones).
   * @param camiones camiones operativos disponibles para la jornada.
   * @return una ruta por cada camión que recibió al menos una entrega. Las rutas quedan disponibles
   *     con sus entregas ya vinculadas ({@link Entrega#asignarRuta(UUID)}).
   */
  @Override
  public List<Ruta> generarRutas(List<Entrega> entregas, List<Camion> camiones) {
    if (entregas == null) {
      throw new ValidationException(ErrorCatalog.GENERADOR_RUTAS_ENTREGAS_NULAS);
    }
    if (camiones == null) {
      throw new ValidationException(ErrorCatalog.GENERADOR_RUTAS_CAMIONES_NULOS);
    }

    List<Camion> camionesDisponibles =
        camiones.stream().filter(Camion::estaDisponibleParaAsignar).toList();

    List<Entrega> entregasOrdenadas = ordenadorEntregas.obtenerEntregasOrdenadas(entregas);
    Map<UUID, List<Entrega>> asignacion =
        asignadorDeEntregas.asignar(entregasOrdenadas, camionesDisponibles);

    LocalDate fechaReparto = LocalDate.now(ZoneId.of("UTC")).plusDays(1);
    List<Ruta> rutas = new ArrayList<>();

    for (Map.Entry<UUID, List<Entrega>> entry : asignacion.entrySet()) {
      UUID camionId = entry.getKey();
      List<Entrega> entregasDelCamion = entry.getValue();
      if (entregasDelCamion.isEmpty()) {
        continue;
      }

      // Nota de diseño: la asignación de chofer excede el alcance de este componente (que sólo
      // conoce entregas y camiones); se utiliza un identificador placeholder que deberá ser
      // reemplazado por el chofer real antes de persistir la ruta (responsabilidad de una capa
      // superior, p. ej. PlanificadorDeEntregas o un futuro ChoferService).
      Ruta ruta = new Ruta(fechaReparto, UUID.randomUUID(), camionId);
      for (Entrega entrega : entregasDelCamion) {
        entrega.asignarRuta(ruta.getId());
        ruta.agregarEntrega(entrega.getId());
      }
      rutas.add(ruta);
    }

    return rutas;
  }
}
