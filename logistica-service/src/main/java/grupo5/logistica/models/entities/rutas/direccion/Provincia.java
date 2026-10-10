package grupo5.logistica.models.entities.rutas.direccion;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public final class Provincia {
  private String nombre;
  private final Pais pais;

  public Provincia(String nombre, Pais pais) {
    this.nombre = nombre;
    this.pais = pais;
  }

  public String nombre() {
    return nombre;
  }

  public Pais pais() {
    return pais;
  }

  public void anonimizar() {
    this.nombre = "ANONIMIZADO";
    if (pais != null) {
      pais.anonimizar();
    }
  }
}
