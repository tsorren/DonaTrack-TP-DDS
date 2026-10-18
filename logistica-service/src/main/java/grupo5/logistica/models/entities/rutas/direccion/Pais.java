package grupo5.logistica.models.entities.rutas.direccion;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public final class Pais {
  private String nombre;

  public Pais(String nombre) {
    this.nombre = nombre;
  }

  public String nombre() {
    return nombre;
  }

  public void anonimizar() {
    this.nombre = "ANONIMIZADO";
  }
}
