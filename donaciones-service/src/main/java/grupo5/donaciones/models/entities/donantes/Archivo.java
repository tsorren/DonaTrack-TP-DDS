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
    this(UUID.randomUUID(), path, EstadoArchivo.PENDIENTE);
  }

  public Archivo(UUID id, String path, EstadoArchivo estado) {
    if (id == null) {
      throw new IllegalArgumentException("El id del archivo no puede ser nulo");
    }
    this.id = id;
    this.path = path;
    this.estado = estado != null ? estado : EstadoArchivo.PENDIENTE;
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
