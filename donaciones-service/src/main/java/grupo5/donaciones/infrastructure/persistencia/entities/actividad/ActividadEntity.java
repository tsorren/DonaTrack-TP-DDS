package grupo5.donaciones.infrastructure.persistencia.entities.actividad;

import grupo5.donaciones.models.entities.actividad.AccionDeAdministracion;
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

/** Una fila del registro de actividad de administración (R11-26, V25). */
@Entity
@Table(name = "actividad_administracion")
@Getter
@Setter
public class ActividadEntity {
  @Id private UUID id;

  @Column(name = "ocurrio_en", nullable = false)
  private LocalDateTime ocurrioEn;

  @Column(name = "autor", nullable = false, columnDefinition = "TEXT")
  private String autor;

  @Enumerated(EnumType.STRING)
  @Column(name = "accion", nullable = false, length = 40)
  private AccionDeAdministracion accion;

  @Column(name = "objeto_id", columnDefinition = "TEXT")
  private String objetoId;

  @Column(name = "detalle", length = 200)
  private String detalle;
}
