package grupo5.donaciones.models.entities.personas;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.privacidad.Anonimizable;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// final para indicar que ninguno más hereda (sino rompe el switch
public final class Juridica extends Persona {
  private final List<Humana> representantes = new ArrayList<>();
  private String razonSocial;
  private TipoJuridico tipo;
  private String rubro;

  public Juridica(Humana representanteInicial) {
    if (representanteInicial == null) {
      throw new ValidationException(ErrorCatalog.JURIDICA_SIN_REPRESENTANTE_INICIAL);
    }
    this.representantes.add(representanteInicial);
  }

  public void agregarRepresentante(Humana representante) {
    if (representante == null) {
      throw new ValidationException(ErrorCatalog.JURIDICA_AGREGAR_REPRESENTANTE_NULO);
    }
    this.representantes.add(representante);
  }

  public void quitarRepresentante(Humana representante) {
    if (!this.representantes.contains(representante)) {
      throw new ValidationException(ErrorCatalog.JURIDICA_QUITAR_REPRESENTANTE_INEXISTENTE);
    }
    if (this.representantes.size() == 1) {
      throw new BusinessStateException(ErrorCatalog.JURIDICA_SIN_REPRESENTANTES_RESTANTES);
    }
    this.representantes.remove(representante);
  }

  @Override
  public TipoPersona getTipoPersona() {
    return TipoPersona.HUMANA;
  }

  @Override
  public void anonimizar() {
    this.razonSocial = Anonimizable.VALOR_STRING;
    // Anonimizamos todos los representantes en la lista
    this.representantes.forEach(Anonimizable::anonimizar);
  }
}
