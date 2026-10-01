package grupo5.incentivos.infrastructure.persistencia.entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("COMPLETITUD")
@Getter
@Setter
@NoArgsConstructor
public class MisionCompletitudEntity extends MisionEntity {

  // FetchType.EAGER justificado: el mapeo a dominio ocurre fuera de la transacción del repo.
  @SuppressWarnings("squid:S1319")
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "mision_categorias_donadas",
      joinColumns = @JoinColumn(name = "mision_id", nullable = false))
  @Column(name = "categoria", nullable = false, length = 255)
  private Set<String> categoriasDonadas = new LinkedHashSet<>();
}
