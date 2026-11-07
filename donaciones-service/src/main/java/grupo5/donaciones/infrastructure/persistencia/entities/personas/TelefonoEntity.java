package grupo5.donaciones.infrastructure.persistencia.entities.personas;

import grupo5.donaciones.models.entities.personas.TipoTelefono;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "telefono")
@Getter
@Setter
public class TelefonoEntity extends MedioDeContactoEntity {
  @Column(name = "caracteristica")
  private String caracteristica;

  @Column(name = "codigo_area")
  private String codigoArea;

  @Column(name = "numero")
  private String numero;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_telefono", nullable = false, length = 10)
  private TipoTelefono tipo;
}
