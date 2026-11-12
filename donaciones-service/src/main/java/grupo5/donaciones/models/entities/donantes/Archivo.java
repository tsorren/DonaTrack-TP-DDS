package grupo5.donaciones.models.entities.donantes;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;

/**
 * Padrón de donantes importado por CSV, con el avance del procesamiento (HB-14): {@code totalFilas}
 * es {@code null} hasta leer el padrón, y cada fila leída termina creada, actualizada o con error.
 * El detalle de las filas con error vive aparte ({@link FilaConError}). {@code nombreOriginal} y
 * {@code subidoEn} (R11-20): el nombre con que se subió y cuándo; null en las importaciones
 * anteriores a que se guardaran (no se inventan).
 */
@Getter
public class Archivo implements AggregateRoot {
  private final UUID id;
  private final String path;
  private final String nombreOriginal;
  private final LocalDateTime subidoEn;
  private EstadoArchivo estado;
  private Integer totalFilas;
  private int creadas;
  private int actualizadas;
  private int conError;
  private final List<UltimoImportado> ultimos = new ArrayList<>();

  /** Cuántos donantes recién procesados se recuerdan para mostrar el avance (R11-01). */
  public static final int ULTIMOS_RECORDADOS = 5;

  /** Largo máximo del nombre guardado (la columna {@code nombre_original}). */
  public static final int LARGO_MAXIMO_NOMBRE = 255;

  /** Sin nombre ni fecha de subida. */
  public Archivo(String path) {
    this(path, null, null);
  }

  /**
   * Un padrón recién subido, con el nombre que mandó el navegador (sin su ruta y recortado a {@link
   * #LARGO_MAXIMO_NOMBRE}) y la hora de la subida.
   */
  public Archivo(String path, String nombreOriginal, LocalDateTime subidoEn) {
    this.id = UUID.randomUUID();
    this.path = path;
    this.nombreOriginal = limpiarNombre(nombreOriginal);
    this.subidoEn = subidoEn;
    this.estado = EstadoArchivo.PENDIENTE;
  }

  /** Constructor de reconstitución para el adaptador JPA */
  public Archivo(
      UUID id,
      String path,
      String nombreOriginal,
      LocalDateTime subidoEn,
      EstadoArchivo estado,
      Integer totalFilas,
      int creadas,
      int actualizadas,
      int conError) {
    this.id = id;
    this.path = path;
    this.nombreOriginal = nombreOriginal;
    this.subidoEn = subidoEn;
    this.estado = estado;
    this.totalFilas = totalFilas;
    this.creadas = creadas;
    this.actualizadas = actualizadas;
    this.conError = conError;
  }

  /** El nombre sin la ruta del navegador ("C:\\datos\\a.csv" es "a.csv"); vacío es null. */
  static String limpiarNombre(String nombre) {
    if (nombre == null) {
      return null;
    }
    String sinRuta =
        nombre.substring(Math.max(nombre.lastIndexOf('/'), nombre.lastIndexOf('\\')) + 1);
    String recortado = sinRuta.strip();
    if (recortado.isEmpty()) {
      return null;
    }
    return recortado.length() > LARGO_MAXIMO_NOMBRE
        ? recortado.substring(0, LARGO_MAXIMO_NOMBRE)
        : recortado;
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

  /** Cuenta la fila creada y recuerda al donante entre los últimos procesados. */
  public void registrarCreada(String nombre) {
    registrarCreada();
    recordar(new UltimoImportado(nombre, UltimoImportado.Resultado.NUEVO));
  }

  public void registrarActualizada() {
    this.actualizadas++;
  }

  /** Cuenta la fila actualizada y recuerda al donante entre los últimos procesados. */
  public void registrarActualizada(String nombre) {
    registrarActualizada();
    recordar(new UltimoImportado(nombre, UltimoImportado.Resultado.ACTUALIZADO));
  }

  /** Los últimos procesados, el más reciente primero; a lo sumo {@link #ULTIMOS_RECORDADOS}. */
  public List<UltimoImportado> getUltimos() {
    return List.copyOf(ultimos);
  }

  /** Reconstitución desde la base, en el orden guardado (el más reciente primero). */
  public void restaurarUltimos(List<UltimoImportado> guardados) {
    ultimos.clear();
    guardados.stream().limit(ULTIMOS_RECORDADOS).forEach(ultimos::add);
  }

  private void recordar(UltimoImportado ultimo) {
    ultimos.add(0, ultimo);
    if (ultimos.size() > ULTIMOS_RECORDADOS) {
      ultimos.remove(ULTIMOS_RECORDADOS);
    }
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
