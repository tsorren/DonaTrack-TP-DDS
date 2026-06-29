package grupo5.logistica.models.entities.chofer;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Chofer {
  private final UUID id;
  private String nombre;
  private String apellido;
  private String licencia;
  private String telefonoContacto;

  public Chofer(String nombre, String apellido, String licencia, String telefonoContacto) {
    this.id = UUID.randomUUID();
    this.nombre = nombre;
    this.apellido = apellido;
    this.licencia = licencia;
    this.telefonoContacto = telefonoContacto;
  }
}
