package grupo5.donaciones.infrastructure.persistencia.entities.donantes;

import grupo5.donaciones.models.entities.donantes.EstadoArchivo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "archivo")
@Getter
@Setter
public class ArchivoEntity {
  @Id private UUID id;

  @Column(name = "path", nullable = false)
  private String path;

  /** El nombre con que se subió (R11-20); null en las importaciones anteriores. */
  @Column(name = "nombre_original", length = 255)
  private String nombreOriginal;

  /** Cuándo se subió, en la hora de Buenos Aires (R11-20); null en las anteriores. */
  @Column(name = "subido_en")
  private LocalDateTime subidoEn;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_archivo", nullable = false, length = 30)
  private EstadoArchivo estado;

  @Column(name = "total_filas")
  private Integer totalFilas;

  @Column(name = "creadas", nullable = false)
  private int creadas;

  @Column(name = "actualizadas", nullable = false)
  private int actualizadas;

  @Column(name = "con_error", nullable = false)
  private int conError;

  /** Los últimos donantes procesados (R11-01): una línea por donante, RESULTADO y nombre. */
  @Column(name = "ultimos", columnDefinition = "TEXT")
  private String ultimos;
}
