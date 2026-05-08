package ar.edu.utn.frba.ddsi.donaciones.models.entities.personas;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Juridica extends Persona {
  private String razonSocial;
  private TipoJuridico tipo;
  private String rubro;
  private List<Humana> representantes;

  public void agregarRepresentante(Humana representante) {
    this.representantes.add(representante);
  }

  public void quitarRepresentante(Humana representante) {
    this.representantes.remove(representante);
  }
}
