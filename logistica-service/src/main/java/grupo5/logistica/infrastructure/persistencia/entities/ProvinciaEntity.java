package grupo5.logistica.infrastructure.persistencia.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "provincia")
@Getter
@Setter
@NoArgsConstructor
public class ProvinciaEntity {

  @Id
  @Column(name = "id_provincia", nullable = false)
  private UUID idProvincia;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "id_pais", nullable = false)
  private PaisEntity pais;

  @Column(name = "nombre", nullable = false, columnDefinition = "TEXT")
  private String nombre;

  public ProvinciaEntity(UUID idProvincia, PaisEntity pais, String nombre) {
    this.idProvincia = idProvincia;
    this.pais = pais;
    this.nombre = nombre;
  }
}
