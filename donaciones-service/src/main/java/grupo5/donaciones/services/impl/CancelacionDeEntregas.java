package grupo5.donaciones.services.impl;

import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.services.logistica.ILogisticaBroker;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * HB-19 (ADR 20261109-cancelacion-de-entregas-que-no-salieron): al dar de baja una necesidad, lo
 * que todavía no salió se cancela y, con la confirmación del proveedor, vuelve al depósito; lo que
 * ya salió llega. La usan la baja de la necesidad y la baja en cascada de su entidad. Repetirla
 * vuelve a pedir las cancelaciones pendientes (cubre una confirmación perdida).
 */
@Component
public class CancelacionDeEntregas {

  private static final String ACTOR_BAJA = "baja-de-necesidad";

  private final IDonacionesIndependientesRepository donacionesRepository;
  private final ILogisticaBroker logisticaBroker;

  public CancelacionDeEntregas(
      IDonacionesIndependientesRepository donacionesRepository, ILogisticaBroker logisticaBroker) {
    this.donacionesRepository = donacionesRepository;
    this.logisticaBroker = logisticaBroker;
  }

  /** Corre dentro de la transacción de la baja. */
  public void cancelarLasQueNoSalieron(UUID necesidadId) {
    for (DonacionIndependiente donacion :
        donacionesRepository.findCancelablesPorNecesidad(necesidadId)) {
      switch (logisticaBroker.pedirCancelacion(donacion.getId())) {
        case PEDIDA -> {
          donacion.pedirCancelacion();
          donacionesRepository.save(donacion);
        }
        case SIN_ENTREGA -> {
          donacion.liberarPorCancelacion(ACTOR_BAJA);
          donacionesRepository.save(donacion);
        }
        case NO_SOPORTADA -> {
          // El proveedor no cancela: la donación sigue su camino (lo loguea el broker)
        }
      }
    }
  }
}
