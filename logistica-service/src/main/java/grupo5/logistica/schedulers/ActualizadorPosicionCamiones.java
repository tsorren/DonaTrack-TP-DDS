package grupo5.logistica.schedulers;

import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.camiones.PosicionCamion;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.repositories.ICamionRepository;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.models.repositories.IRutasRepository;
import grupo5.logistica.services.FuentePosicionCamion;
import grupo5.logistica.services.IEntregasService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cada intervalo pide a la {@link FuentePosicionCamion} la posición del camión de cada ruta en
 * traslado y la guarda en el camión (ADR-E6-07). Si otra escritura del camión gana la carrera de
 * versión, se saltea ese camión: el próximo tick lo reintenta.
 *
 * <p>También pasa a {@code EN_PUERTA} las entregas a cuya puerta llegó el camión y siguen en
 * traslado (HB-22): quien mueve el camión informa la llegada. Si la entidad confirma a la vez, gana
 * su confirmación y la llegada se saltea.
 */
@Component
@ConditionalOnProperty(
    name = "logistica.posicion.simulada.habilitada",
    havingValue = "true",
    matchIfMissing = true)
public class ActualizadorPosicionCamiones {

  private static final Logger log = LoggerFactory.getLogger(ActualizadorPosicionCamiones.class);
  static final String ACTOR = "SIMULADOR";

  private final IRutasRepository rutasRepository;
  private final ICamionRepository camionRepository;
  private final FuentePosicionCamion fuentePosicion;
  private final IEntregasRepository entregasRepository;
  private final IEntregasService entregasService;

  public ActualizadorPosicionCamiones(
      IRutasRepository rutasRepository,
      ICamionRepository camionRepository,
      FuentePosicionCamion fuentePosicion,
      IEntregasRepository entregasRepository,
      IEntregasService entregasService) {
    this.rutasRepository = rutasRepository;
    this.camionRepository = camionRepository;
    this.fuentePosicion = fuentePosicion;
    this.entregasRepository = entregasRepository;
    this.entregasService = entregasService;
  }

  @Scheduled(
      fixedDelayString = "${logistica.posicion.intervalo-ms:30000}",
      initialDelayString = "${logistica.posicion.intervalo-ms:30000}")
  public void actualizar() {
    for (Ruta ruta : rutasRepository.findByEstado(EstadoRuta.EN_TRASLADO)) {
      fuentePosicion.posicionActual(ruta).ifPresent(posicion -> guardar(ruta, posicion));
      fuentePosicion.entregasAlcanzadas(ruta).forEach(this::marcarEnPuerta);
    }
  }

  private void marcarEnPuerta(UUID idEntrega) {
    boolean enTraslado =
        entregasRepository
            .findById(idEntrega)
            .filter(e -> e.getEstadoActual() == EstadoEntrega.EN_TRASLADO)
            .isPresent();
    if (!enTraslado) {
      return;
    }
    try {
      entregasService.marcarEnPuerta(idEntrega, ACTOR);
    } catch (OptimisticLockingFailureException | ValidationException e) {
      // La entidad confirmó entre la lectura y la escritura: la llegada ya no hace falta
      log.warn("La llegada de la entrega {} se saltea: {}", idEntrega, e.getMessage());
    }
  }

  private void guardar(Ruta ruta, PosicionCamion posicion) {
    if (ruta.getCamionId() == null) {
      return;
    }
    try {
      camionRepository
          .findById(ruta.getCamionId())
          .filter(camion -> camion.registrarPosicion(posicion))
          .ifPresent(camionRepository::save);
    } catch (OptimisticLockingFailureException e) {
      log.warn(
          "Posición del camión {} no guardada por conflicto de versión; se reintenta en el próximo tick",
          ruta.getCamionId());
    }
  }
}
