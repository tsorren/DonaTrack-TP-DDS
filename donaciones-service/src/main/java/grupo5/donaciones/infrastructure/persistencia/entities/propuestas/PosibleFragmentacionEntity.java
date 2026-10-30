package grupo5.donaciones.infrastructure.persistencia.entities.propuestas;

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
@Table(name = "posible_fragmentacion")
@Getter
@Setter
public class PosibleFragmentacionEntity {
  @Id private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_propuesta", nullable = false)
  private PropuestaEntity propuesta;

  // D-23: FK a la donación independiente; en el dominio, una referencia por id
  @Column(name = "id_donacion_independiente", nullable = false)
  private UUID donacionIndependienteId;

  // D-8: INTEGER, como el dominio
  @Column(name = "cantidad", nullable = false)
  private Integer cantidad;
}
