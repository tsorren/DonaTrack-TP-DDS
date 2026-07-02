package grupo5.donaciones.models.entities.necesidades;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
import grupo5.donaciones.dto.NecesidadDTO;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.Getter;

@Getter
public abstract class Necesidad implements Asignable, AggregateRoot {
  private final UUID id;
  private UUID subcategoriaId;
  private Integer cantidadNecesitada;
  private String descripcion;
  private LocalDate fechaInicio;
  private UUID entidadId;

  protected Necesidad(UUID subcategoriaId, Integer cantidadNecesitada, String descripcion) {
    this.id = UUID.randomUUID();
    this.subcategoriaId = subcategoriaId;
    this.cantidadNecesitada = cantidadNecesitada;
    this.descripcion = descripcion;
    this.fechaInicio = LocalDate.now(ZoneId.systemDefault());

    validarNecesidad();
  }

  public void asociarAEntidad(UUID entidadId) {
    if (entidadId == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    this.entidadId = entidadId;
  }

  public void actualizarCantidadNecesitada(Integer cantidadNecesitada) {
    if (cantidadNecesitada == null || cantidadNecesitada <= 0) {
      throw new ValidationException(ErrorCatalog.CANTIDAD_NECESITADA_INVALIDA);
    }
    this.cantidadNecesitada = cantidadNecesitada;
  }

  public abstract TipoNecesidad getTipoNecesidad();

  protected NecesidadDTO toDTO(LocalDate fechaFin) {
    return new NecesidadDTO(
        this.id,
        this.getTipoNecesidad().name(),
        this.entidadId, // Otro aggregate root -> ref por id
        this.subcategoriaId, // Otro aggregate root -> ref por id
        this.cantidadNecesitada,
        this.descripcion,
        this.estaSatisfecha(),
        this.fechaInicio,
        fechaFin);
  }

  public abstract NecesidadDTO toDTO();

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

  public abstract List<DonacionIndependiente> getDonacionesAsignadas();

  @Override
  public Necesidad obtenerNecesidad() {
    return this;
  }

  public abstract void asignarDonacion(DonacionIndependiente donacionAsignada);

  public abstract void quitarDonacion(DonacionIndependiente donacionAsignada);

  public abstract boolean estaSatisfecha();

  public abstract boolean isActiva();

  public abstract Integer cantidadAcumulada();
}
