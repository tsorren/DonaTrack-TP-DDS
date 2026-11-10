package grupo5.logistica.schedulers;

import grupo5.logistica.models.entities.camiones.PosicionCamion;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.repositories.ICamionRepository;
import grupo5.logistica.models.repositories.IRutasRepository;
import grupo5.logistica.services.FuentePosicionCamion;
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
 */
@Component
@ConditionalOnProperty(
    name = "logistica.posicion.simulada.habilitada",
    havingValue = "true",
    matchIfMissing = true)
public class ActualizadorPosicionCamiones {

  private static final Logger log = LoggerFactory.getLogger(ActualizadorPosicionCamiones.class);

  private final IRutasRepository rutasRepository;
  private final ICamionRepository camionRepository;
  private final FuentePosicionCamion fuentePosicion;

  public ActualizadorPosicionCamiones(
      IRutasRepository rutasRepository,
      ICamionRepository camionRepository,
      FuentePosicionCamion fuentePosicion) {
    this.rutasRepository = rutasRepository;
    this.camionRepository = camionRepository;
    this.fuentePosicion = fuentePosicion;
  }

  @Scheduled(
      fixedDelayString = "${logistica.posicion.intervalo-ms:30000}",
      initialDelayString = "${logistica.posicion.intervalo-ms:30000}")
  public void actualizar() {
    for (Ruta ruta : rutasRepository.findByEstado(EstadoRuta.EN_TRASLADO)) {
      fuentePosicion.posicionActual(ruta).ifPresent(posicion -> guardar(ruta, posicion));
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
