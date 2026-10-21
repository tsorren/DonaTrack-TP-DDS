package grupo5.incentivos.infrastructure.persistencia.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Insignia asociada a una misión. Si todas las columnas son nulas, la misión no tiene insignia. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InsigniaEmbeddable {

  @Column(name = "insignia_nombre", length = 255)
  private String nombre;

  @Column(name = "insignia_descripcion")
  private String descripcion;

  @Column(name = "insignia_imagen_url")
  private String imagenUrl;
}
