package grupo5.logistica.services;

import grupo5.common.exceptions.DonaTrackException;
import grupo5.common.exceptions.ErrorCatalog;

/**
 * Una foto de recepción no pasó la validación (ADR-E6-08). El {@link Motivo} dice por qué; el
 * controller de fotos lo traduce al status HTTP (413, 415 o 400).
 */
public class FotoRecepcionRechazadaException extends DonaTrackException {

  /** Por qué se rechazó la foto. */
  public enum Motivo {
    /** Más de 5 MB o más píxeles de los que se aceptan. */
    DEMASIADO_GRANDE("La foto supera el tamaño máximo permitido."),
    /** Los primeros bytes no son de un JPEG ni de un PNG. */
    TIPO_NO_PERMITIDO("La foto tiene que ser JPEG o PNG."),
    /** Tiene la cabecera de JPEG o PNG, pero no se puede decodificar. */
    ILEGIBLE("La foto no se puede leer como imagen.");

    private final String detalle;

    Motivo(String detalle) {
      this.detalle = detalle;
    }

    public String detalle() {
      return detalle;
    }
  }

  private final Motivo motivo;

  public FotoRecepcionRechazadaException(Motivo motivo) {
    super(ErrorCatalog.ARGUMENTO_INVALIDO);
    this.motivo = motivo;
  }

  public FotoRecepcionRechazadaException(Motivo motivo, Throwable causa) {
    super(ErrorCatalog.ARGUMENTO_INVALIDO, causa);
    this.motivo = motivo;
  }

  public Motivo getMotivo() {
    return motivo;
  }
}
