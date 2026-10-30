package grupo5.donaciones.infrastructure.persistencia.entities.donantes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** El id es el de la persona (#892); la FK hacia {@code persona} la declara V8. */
@Entity
@Table(name = "donante")
@Getter
@Setter
public class DonanteEntity {
  @Id private UUID id;

  @Column(name = "activo", nullable = false)
  private boolean activo;
}
