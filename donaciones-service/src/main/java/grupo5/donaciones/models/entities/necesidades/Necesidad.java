package grupo5.donaciones.models.entities.necesidades;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
import grupo5.donaciones.dto.NecesidadDTO;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Necesidad implements Asignable, AggregateRoot {
  private final UUID id;
  private Subcategoria subcategoria;
  private Integer cantidadNecesitada;
  private String descripcion;
  private LocalDate fechaInicio;
  private EntidadBeneficiaria entidad;

  protected Necesidad(Subcategoria subcategoria, Integer cantidadNecesitada, String descripcion) {
    this.id = UUID.randomUUID();
    this.subcategoria = subcategoria;
    this.cantidadNecesitada = cantidadNecesitada;
    this.descripcion = descripcion;
    this.fechaInicio = LocalDate.now(ZoneId.systemDefault());

    validarNecesidad();
  }

  public abstract TipoNecesidad getTipoNecesidad();

  protected NecesidadDTO toDTO(LocalDate fechaFin) {
    return new NecesidadDTO(
        this.id,
        this.getTipoNecesidad().name(),
        this.getEntidad() != null
            ? this.getEntidad().getId()
            : null, // Otro aggregate root -> ref por id
        this.subcategoria != null
            ? this.subcategoria.getId()
            : null, // Otro aggregate root -> ref por id
        this.cantidadNecesitada,
        this.descripcion,
        this.estaSatisfecha(),
        this.fechaInicio,
        fechaFin);
  }

  public abstract NecesidadDTO toDTO();

  private void validarNecesidad() {

    if (this.subcategoria == null) {
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

  public abstract Integer cantidadAcumulada();
}
