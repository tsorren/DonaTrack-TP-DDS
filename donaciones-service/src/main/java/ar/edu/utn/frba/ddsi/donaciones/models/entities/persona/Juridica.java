package ar.edu.utn.frba.ddsi.donaciones.models.entities.persona;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

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
