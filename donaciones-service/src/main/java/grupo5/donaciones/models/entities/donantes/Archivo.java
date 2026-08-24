package grupo5.donaciones.models.entities.donantes;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
import java.util.UUID;
import lombok.Getter;

@Getter
public class Archivo implements AggregateRoot {
  private final UUID id;
  private String path;
  private EstadoArchivo estado;

  public Archivo(String path) {
    this.id = UUID.randomUUID();
    this.path = path;
    this.estado = EstadoArchivo.PENDIENTE;
  }

  public void marcarComoProcesando() {
    this.estado = EstadoArchivo.PROCESANDO;
  }

  public void marcarComoProcesado() {

    if (this.estado != EstadoArchivo.PROCESANDO) {
      throw new ValidationException(ErrorCatalog.ESTADO_ARCHIVO_INVALIDO);
    }
    this.estado = EstadoArchivo.PROCESADO;
  }

  public void finalizarProcesamiento(int erroresDeNegocio) {
    if (erroresDeNegocio > 0) {
      marcarComoCompletadoConErrores();
    } else {
      marcarComoProcesado();
    }
  }

  public void marcarComoError() {
    this.estado = EstadoArchivo.ERROR;
  }

  public void marcarComoCompletadoConErrores() {
    if (this.estado != EstadoArchivo.PROCESANDO) {
      throw new ValidationException(ErrorCatalog.ESTADO_ARCHIVO_INVALIDO);
    }
    this.estado = EstadoArchivo.PROCESADO_CON_ERRORES;
  }

  @Override
  public UUID getId() {
    return this.id;
  }
}
