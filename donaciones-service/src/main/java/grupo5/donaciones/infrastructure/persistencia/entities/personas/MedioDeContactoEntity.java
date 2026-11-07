package grupo5.donaciones.infrastructure.persistencia.entities.personas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "medio_de_contacto")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
public abstract class MedioDeContactoEntity {
  // El dominio no le da identidad a un medio de contacto: se genera al crear la fila
  @Id private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_persona", nullable = false)
  private PersonaEntity persona;

  @Column(name = "es_predeterminado", nullable = false)
  private boolean esPredeterminado;
}
