package grupo5.logistica.infrastructure.persistencia.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pais")
@Getter
@Setter
@NoArgsConstructor
public class PaisEntity {

  @Id
  @Column(name = "id_pais", nullable = false)
  private UUID idPais;

  @Column(name = "nombre", nullable = false, unique = true, columnDefinition = "TEXT")
  private String nombre;

  public PaisEntity(UUID idPais, String nombre) {
    this.idPais = idPais;
    this.nombre = nombre;
  }
}
