package grupo5.incentivos.infrastructure.persistencia.entities;

import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
public class CambioCategoriaEmbeddable {

  @Enumerated(EnumType.STRING)
  @Column(name = "categoria_anterior", nullable = false, length = 20)
  private CategoriaDonante anterior;

  @Enumerated(EnumType.STRING)
  @Column(name = "categoria_nueva", nullable = false, length = 20)
  private CategoriaDonante nueva;

  @Column(name = "fecha", nullable = false)
  private LocalDate fecha;
}
