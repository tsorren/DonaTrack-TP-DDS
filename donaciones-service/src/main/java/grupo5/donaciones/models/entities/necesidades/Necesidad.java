package grupo5.donaciones.models.entities.necesidades;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Necesidad {
  private Subcategoria subcategoria;
  private Integer cantidadNecesitada;
  private String descripcion;
  private LocalDate fechaInicio;
  private EntidadBeneficiaria entidad;
  private List<DonacionIndependiente> donacionesAsignadas;

  protected Necesidad(Subcategoria subcategoria, Integer cantidadNecesitada, String descripcion) {
    this.subcategoria = subcategoria;
    this.cantidadNecesitada = cantidadNecesitada;
    this.descripcion = descripcion;
    this.fechaInicio = LocalDate.now(ZoneId.systemDefault());

    validarNecesidad();
  }

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

  public abstract void asignarDonacion(DonacionIndependiente donacionAsignada);

  public abstract void quitarDonacion(DonacionIndependiente donacionAsignada);

  public abstract boolean estaSatisfecha();

  public abstract Integer cantidadAcumulada();
}
