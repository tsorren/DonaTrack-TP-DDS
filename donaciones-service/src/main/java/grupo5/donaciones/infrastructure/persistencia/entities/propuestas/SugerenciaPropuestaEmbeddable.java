package grupo5.donaciones.infrastructure.persistencia.entities.propuestas;

import grupo5.donaciones.models.entities.propuestas.TipoAlgoritmo;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Una fila de {@code sugerencia_propuesta} (V24, R11-24): qué algoritmo y en qué puesto. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SugerenciaPropuestaEmbeddable {

  @Enumerated(EnumType.STRING)
  @Column(name = "algoritmo", nullable = false, length = 30)
  private TipoAlgoritmo algoritmo;

  @Column(name = "puesto", nullable = false)
  private Integer puesto;

  @Column(name = "de_cuantas", nullable = false)
  private Integer deCuantas;

  @Column(name = "eligio_las_donaciones", nullable = false)
  private Boolean eligioLasDonaciones;
}
