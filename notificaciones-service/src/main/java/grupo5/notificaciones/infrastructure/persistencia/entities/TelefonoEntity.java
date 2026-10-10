package grupo5.notificaciones.infrastructure.persistencia.entities;

import grupo5.notificaciones.models.entities.personas.TipoTelefono;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("TELEFONO")
@Getter
@Setter
@NoArgsConstructor
public class TelefonoEntity extends MedioDeContactoEntity {
  @Column(name = "caracteristica", length = 10)
  private String caracteristica;

  @Column(name = "codigo_area", length = 10)
  private String codigoArea;

  @Column(name = "numero", length = 20)
  private String numero;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_telefono", length = 20)
  private TipoTelefono tipo = TipoTelefono.ESTANDAR;
}
