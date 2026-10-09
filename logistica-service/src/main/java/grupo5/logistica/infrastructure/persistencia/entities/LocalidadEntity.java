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
@Table(name = "localidad")
@Getter
@Setter
@NoArgsConstructor
public class LocalidadEntity {

  @Id
  @Column(name = "id_localidad", nullable = false)
  private UUID idLocalidad;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "id_provincia", nullable = false)
  private ProvinciaEntity provincia;

  @Column(name = "nombre", nullable = false, columnDefinition = "TEXT")
  private String nombre;

  public LocalidadEntity(UUID idLocalidad, ProvinciaEntity provincia, String nombre) {
    this.idLocalidad = idLocalidad;
    this.provincia = provincia;
    this.nombre = nombre;
  }
}
