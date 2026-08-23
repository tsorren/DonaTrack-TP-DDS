package grupo5.logistica.models.entities.camiones;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import java.util.Optional;

public final class GestorDeCamiones {

  private GestorDeCamiones() {}

  public static Optional<Camion> procesarSolicitudNuevoCamion(SolicitudNuevoCamion solicitud) {
    if (solicitud == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }

    ValidadorPatentes.validar(solicitud.patente(), solicitud.patentesExistentes());
    return Optional.of(
        new Camion(
            solicitud.patente(),
            solicitud.capacidadVolumen(),
            solicitud.capacidadKG(),
            solicitud.altura()));
  }

  public static void cambiarEstado(Camion camion, EstadoCamion estadoNuevo) {
    if (camion == null || estadoNuevo == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }

    switch (estadoNuevo) {
      case DISPONIBLE -> camion.habilitar();
      case DESHABILITADO -> camion.deshabilitar();
      case EN_RUTA -> throw new ValidationException(ErrorCatalog.ESTADO_CAMION_TRANSICION_INVALIDA);
    }
  }
}
