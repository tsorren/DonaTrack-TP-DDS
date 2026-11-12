package grupo5.donaciones.models.entities.necesidades;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import lombok.Getter;

@Getter
public abstract class Necesidad implements AggregateRoot {
  private final UUID id;
  private final UUID subcategoriaId;
  private Integer cantidadNecesitada;
  private String descripcion;
  private final LocalDate fechaInicio;
  private UUID entidadId;

  /** Versión de la fila al leerla (bloqueo optimista). Nula mientras no se guardó. */
  private Long version;

  /** La cantidad antes del último cambio y el día del cambio; nulos si nunca cambió. */
  private Integer cantidadAnterior;

  private LocalDate fechaUltimoCambio;

  protected Necesidad(UUID subcategoriaId, Integer cantidadNecesitada, String descripcion) {
    this.id = UUID.randomUUID();
    this.subcategoriaId = subcategoriaId;
    this.cantidadNecesitada = cantidadNecesitada;
    this.descripcion = descripcion;
    this.fechaInicio = LocalDate.now(ZoneId.systemDefault());

    validarNecesidad();
  }

  /** Constructor de reconstitución para el adaptador JPA */
  protected Necesidad(DatosNecesidad datos) {
    this.id = datos.id();
    this.subcategoriaId = datos.subcategoriaId();
    this.cantidadNecesitada = datos.cantidadNecesitada();
    this.descripcion = datos.descripcion();
    this.fechaInicio = datos.fechaInicio();
    this.entidadId = datos.entidadId();
    this.version = datos.version();
    this.cantidadAnterior = datos.cantidadAnterior();
    this.fechaUltimoCambio = datos.fechaUltimoCambio();
  }

  /** Lo asigna el adaptador de persistencia después de guardar, con la versión nueva de la fila. */
  public void asignarVersion(Long version) {
    this.version = version;
  }

  public void asociarAEntidad(UUID entidadId) {
    if (entidadId == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    this.entidadId = entidadId;
  }

  public void actualizar(String descripcion, Integer cantidadNecesitada) {
    if (cantidadNecesitada != null) {
      actualizarCantidadNecesitada(cantidadNecesitada);
    }
    if (descripcion != null && !descripcion.trim().isEmpty()) {
      this.descripcion = descripcion.trim();
    }
  }

  public void actualizarCantidadNecesitada(Integer cantidadNecesitada) {
    if (cantidadNecesitada == null || cantidadNecesitada <= 0) {
      throw new ValidationException(ErrorCatalog.CANTIDAD_NECESITADA_INVALIDA);
    }
    if (!cantidadNecesitada.equals(this.cantidadNecesitada)) {
      this.cantidadAnterior = this.cantidadNecesitada;
      this.fechaUltimoCambio = LocalDate.now(ZoneId.systemDefault());
    }
    this.cantidadNecesitada = cantidadNecesitada;
  }

  public abstract void desactivar();

  public abstract TipoNecesidad getTipoNecesidad();

  /**
   * Estado calculado en cada lectura (ADR 20261102 del ciclo de vida, punto 1). {@code recibido}:
   * la suma de las donaciones entregadas, que la necesidad no conoce (DTI-06).
   */
  public abstract EstadoNecesidad estado(int recibido, LocalDate hoy);

  /**
   * Si puede recibir asignaciones: activa y, en una extraordinaria, sin vencer. Una necesidad
   * {@link EstadoNecesidad#TERMINADA} deja de recibirlas.
   */
  public boolean admiteAsignaciones(LocalDate hoy) {
    return isActiva();
  }

  /**
   * Si la salida muestra el último cambio de cantidad (el cartel de la entidad, ADR 20261102 punto
   * 5). En una recurrente, solo mientras dure el período en que se hizo.
   */
  public boolean muestraUltimoCambio() {
    return fechaUltimoCambio != null;
  }

  /** Fin del período en curso: {@code null} en una extraordinaria. */
  public LocalDate finDelPeriodoActual() {
    return null;
  }

  private void validarNecesidad() {

    if (this.subcategoriaId == null) {
      throw new ValidationException(ErrorCatalog.NECESIDAD_SIN_SUBCATEGORIA);
    }

    if (this.cantidadNecesitada == null || this.cantidadNecesitada <= 0) {
      throw new ValidationException(ErrorCatalog.CANTIDAD_NECESITADA_INVALIDA);
    }

    if (this.descripcion == null || this.descripcion.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.DESCRIPCION_NECESIDAD_VACIA);
    }
  }

  /**
   * A dónde va una donación que se le asigna. En una recurrente incluye el período actual; si no
   * hay período, no se puede asignar.
   */
  public abstract DestinoAsignacion destinoDeAsignacion();

  /** El período al que se suman las asignaciones: {@code null} en una extraordinaria. */
  public abstract UUID periodoActualId();

  /**
   * Lo asignado lo calcula quien llama (suma de las donaciones con este destino), porque la
   * necesidad no conoce sus donaciones (DTI-06).
   */
  public abstract boolean estaSatisfechaCon(int cantidadAcumulada);

  public int cantidadRestante(int cantidadAcumulada) {
    return Math.max(0, this.cantidadNecesitada - cantidadAcumulada);
  }

  public abstract boolean isActiva();
}
