package grupo5.donaciones.infrastructure.persistencia.entities.donantes;

import grupo5.donaciones.models.entities.donantes.EstadoArchivo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_archivo", nullable = false, length = 30)
  private EstadoArchivo estado;
}
