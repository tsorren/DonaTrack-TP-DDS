package ar.edu.utn.frba.ddsi.donaciones.models.entities.personas;

import ar.edu.utn.frba.ddsi.donaciones.models.privacidad.Anonimizable;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Juridica extends Persona {
  private String razonSocial;
  private TipoJuridico tipo;
  private String rubro;
  private final List<Humana> representantes = new ArrayList<>();

  public Juridica(Humana representanteInicial) {
    if (representanteInicial == null) {
      throw new IllegalArgumentException(
          "Toda persona jurídica debe tener al menos un representante al registrarse.");
    }
    this.representantes.add(representanteInicial);
  }

  public void agregarRepresentante(Humana representante) {
    if (representante == null) {
      throw new IllegalArgumentException("El representante a agregar no puede ser nulo.");
    }
    this.representantes.add(representante);
  }

  public void quitarRepresentante(Humana representante) {
    if (!this.representantes.contains(representante)) {
      throw new IllegalArgumentException(
          "El representante no pertenece a la lista de esta entidad jurídica.");
    }
    if (this.representantes.size() == 1) {
      throw new IllegalStateException("La entidad jurídica no puede quedarse sin representantes.");
    }
    this.representantes.remove(representante);
  }

  @Override
  public void anonimizar() {
    this.razonSocial = "ANONIMIZADO";
    // Anonimizamos todos los representantes en la lista
    this.representantes.forEach(Anonimizable::anonimizar);
  }
}
