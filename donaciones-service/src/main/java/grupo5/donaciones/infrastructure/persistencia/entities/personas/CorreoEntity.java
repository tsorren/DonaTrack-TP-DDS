package grupo5.donaciones.infrastructure.persistencia.entities.personas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "correo")
@Getter
@Setter
public class CorreoEntity extends MedioDeContactoEntity {
  @Column(name = "direccion_correo")
  private String direccionCorreo;
}
