package grupo5.notificaciones.infrastructure.persistencia.entities;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "medio_de_contacto")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_medio", discriminatorType = DiscriminatorType.STRING, length = 20)
@Getter
@Setter
public class MedioDeContactoEntity {
  @Id private UUID id;

  @Column(name = "es_predeterminado", nullable = false)
  private Boolean esPredeterminado;

  protected MedioDeContactoEntity() {
    this.id = UUID.randomUUID();
    this.esPredeterminado = false;
  }
}
