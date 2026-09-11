package grupo5.logistica.models.entities.rutas.direccion;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public final class Localidad {
  private String nombre;
  private final Provincia provincia;

  public Localidad(String nombre, Provincia provincia) {
    this.nombre = nombre;
    this.provincia = provincia;
  }

  public String nombre() {
    return nombre;
  }

  public Provincia provincia() {
    return provincia;
  }

  public void anonimizar() {
    this.nombre = "ANONIMIZADO";
    if (provincia != null) {
      provincia.anonimizar();
    }
  }
}
