package grupo5.donaciones.models.entities.propuestas;

import com.fasterxml.jackson.annotation.JsonIgnore;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PosibleFragmentacion {
  @JsonIgnore private DonacionIndependiente donacionOriginal;
  private UUID donacionOriginalId;
  private Integer cantidadNecesaria;

  public PosibleFragmentacion() {}

  /** Constructor de reconstitución para el adaptador JPA: la donación se busca al confirmar. */
  public PosibleFragmentacion(UUID donacionOriginalId, Integer cantidadNecesaria) {
    this.donacionOriginalId = donacionOriginalId;
    this.cantidadNecesaria = cantidadNecesaria;
  }

  public PosibleFragmentacion(DonacionIndependiente donacionOriginal, Integer cantidadNecesaria) {
    this.donacionOriginal = donacionOriginal;
    this.donacionOriginalId = donacionOriginal != null ? donacionOriginal.getId() : null;
    this.cantidadNecesaria = cantidadNecesaria;
  }

  public DonacionIndependiente confirmar(Necesidad necesidad, String actor) {
    if (necesidad == null || this.donacionOriginal == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    if (this.cantidadNecesaria == null || this.cantidadNecesaria <= 0) {
      throw new ValidationException(ErrorCatalog.PROPUESTA_FRAGMENTACION_CANTIDAD_INVALIDA);
    }

    String actorFinal = (actor != null && !actor.isBlank()) ? actor : "SISTEMA";
    DonacionIndependiente donacionAsignar;

    if (this.donacionOriginal.getCantidad() > this.cantidadNecesaria) {
      donacionAsignar = this.donacionOriginal.fragmentarse(this.cantidadNecesaria);
    } else {
      donacionAsignar = this.donacionOriginal;
    }

    // La necesidad no guarda sus donaciones (DTI-06): la donación registra a dónde se asignó
    donacionAsignar.asignar(actorFinal, necesidad.destinoDeAsignacion());
    return donacionAsignar;
  }
}
