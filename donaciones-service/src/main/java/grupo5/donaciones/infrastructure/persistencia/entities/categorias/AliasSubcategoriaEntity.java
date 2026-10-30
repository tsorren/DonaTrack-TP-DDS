package grupo5.donaciones.infrastructure.persistencia.entities.categorias;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "alias")
@Getter
@Setter
public class AliasSubcategoriaEntity {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_subcategoria", nullable = false)
  private SubcategoriaEntity subcategoria;

  @Column(name = "alias", nullable = false)
  private String alias;

  public AliasSubcategoriaEntity() {
    this.id = UUID.randomUUID();
  }
}
