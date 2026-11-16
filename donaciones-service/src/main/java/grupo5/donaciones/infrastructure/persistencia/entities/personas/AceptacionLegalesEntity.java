package grupo5.donaciones.infrastructure.persistencia.entities.personas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Constancia de los legales (HB-20, V19). */
@Entity
@Table(name = "aceptacion_legales")
@Getter
@Setter
public class AceptacionLegalesEntity {
  @Id private UUID id;

  @Column(name = "persona_id", nullable = false)
  private UUID personaId;

  @Column(name = "version", nullable = false)
  private int version;

  @Column(name = "aceptada_en", nullable = false)
  private LocalDateTime aceptadaEn;
}
