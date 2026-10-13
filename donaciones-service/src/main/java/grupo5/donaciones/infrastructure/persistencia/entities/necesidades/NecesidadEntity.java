package grupo5.donaciones.infrastructure.persistencia.entities.necesidades;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "necesidad")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_necesidad", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
public abstract class NecesidadEntity {
  @Id private UUID id;

  @Column(name = "subcategoria_id", nullable = false)
  private UUID subcategoriaId;

  @Column(name = "entidad_beneficiaria_id")
  private UUID entidadBeneficiariaId;

  @Column(name = "cantidad_necesitada", nullable = false)
  private Long cantidadNecesitada;

  @Column(name = "descripcion", nullable = false, length = 500)
  private String descripcion;

  @Column(name = "fecha_inicio", nullable = false)
  private LocalDateTime fechaInicio;

  @Column(name = "activa", nullable = false)
  private boolean activa;
}
