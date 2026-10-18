package grupo5.notificaciones.infrastructure.persistencia.entities;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("CORREO")
@Getter
@Setter
@NoArgsConstructor
public class CorreoEntity extends MedioDeContactoEntity {
  @Column(name = "direccion_correo", length = 150)
  private String direccionCorreo;
}
