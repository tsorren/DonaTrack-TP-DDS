package grupo5.common;

import lombok.AllArgsConstructor;

@Getter
@Setter
@AllArgsConstructor
public class Persona {
  private String nombre;
  private String apellido;

  public String nombreCompleto() {
    return this.nombre + " " + this.apellido;
  }
}
