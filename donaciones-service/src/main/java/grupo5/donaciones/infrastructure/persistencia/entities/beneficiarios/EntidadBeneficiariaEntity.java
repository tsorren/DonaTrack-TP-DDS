package grupo5.donaciones.infrastructure.persistencia.entities.beneficiarios;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** El id es el de la jurídica (#892); la FK hacia {@code juridica} la declara V8. */
@Entity
@Table(name = "entidad_beneficiaria")
@Getter
@Setter
public class EntidadBeneficiariaEntity {
  @Id private UUID id;

  @Column(name = "activo", nullable = false)
  private boolean activo;
}
