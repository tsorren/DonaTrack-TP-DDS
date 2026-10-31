package grupo5.donaciones.infrastructure.persistencia.entities.categorias;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "subcategoria")
@Getter
@Setter
public class SubcategoriaEntity {
  @Id private UUID id;

  // El dominio referencia la categoría por id (otro aggregate root); la FK vive en la migración.
  @Column(name = "id_categoria", nullable = false)
  private UUID categoriaId;

  @Column(name = "nombre", nullable = false)
  private String nombre;

  @OneToMany(
      mappedBy = "subcategoria",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  private List<AliasSubcategoriaEntity> aliases = new ArrayList<>();
}
