package grupo5.donaciones.infrastructure.persistencia.entities.categorias;

import grupo5.donaciones.models.entities.categorias.Unidad;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "categoria")
@Getter
@Setter
public class CategoriaEntity {
  @Id private UUID id;

  @Column(name = "nombre", nullable = false)
  private String nombre;

  @Column(name = "con_uso", nullable = false)
  private boolean conUso;

  @Column(name = "con_vencimiento", nullable = false)
  private boolean conVencimiento;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_unidad", nullable = false, length = 20)
  private Unidad tipoUnidad;
}
