package grupo5.incentivos.infrastructure.persistencia.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InsigniaGanadaEmbeddable {

  @Column(name = "nombre", nullable = false, length = 255)
  private String nombre;

  @Column(name = "descripcion")
  private String descripcion;

  @Column(name = "imagen_url")
  private String imagenUrl;

  @Column(name = "visible", nullable = false)
  private boolean visible;

  @Column(name = "fecha_obtenida")
  private LocalDate fechaObtenida;
}
