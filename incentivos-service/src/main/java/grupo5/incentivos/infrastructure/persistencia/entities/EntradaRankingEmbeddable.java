package grupo5.incentivos.infrastructure.persistencia.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EntradaRankingEmbeddable {

  @Column(name = "posicion", nullable = false)
  private int posicion;

  // Sin FK al donante: el ranking es histórico y sobrevive al donante.
  @Column(name = "donante_id", nullable = false)
  private UUID donanteId;

  @Column(name = "nombre_donante", length = 255)
  private String nombreDonante;

  @Column(name = "misiones_completadas", nullable = false)
  private long misionesCompletadas;
}
