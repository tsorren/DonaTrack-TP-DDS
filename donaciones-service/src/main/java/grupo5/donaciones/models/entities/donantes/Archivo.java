package grupo5.donaciones.models.entities.donantes;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
import java.util.UUID;
import lombok.Getter;

/**
 * Padrón de donantes importado por CSV, con el avance del procesamiento (HB-14): {@code totalFilas}
 * es {@code null} hasta leer el padrón, y cada fila leída termina creada, actualizada o con error.
 * El detalle de las filas con error vive aparte ({@link FilaConError}).
 */
@Getter
public class Archivo implements AggregateRoot {
  private final UUID id;
  private final String path;
  private EstadoArchivo estado;
  private Integer totalFilas;
  private int creadas;
  private int actualizadas;
  private int conError;

  public Archivo(String path) {
    this.id = UUID.randomUUID();
    this.path = path;
    this.estado = EstadoArchivo.PENDIENTE;
  }

  /** Constructor de reconstitución para el adaptador JPA */
  public Archivo(
      UUID id,
      String path,
      EstadoArchivo estado,
      Integer totalFilas,
      int creadas,
      int actualizadas,
      int conError) {
    this.id = id;
    this.path = path;
    this.estado = estado;
    this.totalFilas = totalFilas;
    this.creadas = creadas;
    this.actualizadas = actualizadas;
    this.conError = conError;
  }

  public void marcarComoProcesando() {
    this.estado = EstadoArchivo.PROCESANDO;
  }

  /** Fija el total de filas leídas del padrón (sin el encabezado ni las líneas en blanco). */
  public void iniciarConteo(int totalFilas) {
    this.totalFilas = totalFilas;
  }

  public void registrarCreada() {
    this.creadas++;
  }

  public void registrarActualizada() {
    this.actualizadas++;
  }

  public void registrarError() {
    this.conError++;
  }

  /** Filas ya resueltas, con cualquier resultado. */
  public int getProcesadas() {
    return creadas + actualizadas + conError;
  }

  public void marcarComoProcesado() {

    if (this.estado != EstadoArchivo.PROCESANDO) {
      throw new ValidationException(ErrorCatalog.ESTADO_ARCHIVO_INVALIDO);
    }
    this.estado = EstadoArchivo.PROCESADO;
  }

  public void finalizarProcesamiento() {
    if (conError > 0) {
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
