package grupo5.incentivos.infrastructure.persistencia.entities;

import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "mision")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_mision", length = 20)
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class MisionEntity {

  @Id @EqualsAndHashCode.Include private UUID id;

  @Column(name = "numero_mision")
  private Integer numeroMision;

  @Column(name = "nombre", nullable = false, length = 255)
  private String nombre;

  @Column(name = "descripcion")
  private String descripcion;

  @Enumerated(EnumType.STRING)
  @Column(name = "categoria", nullable = false, length = 20)
  private CategoriaDonante categoria;

  @Column(name = "objetivo", nullable = false)
  private Integer objetivo;

  @Column(name = "progreso_actual", nullable = false)
  private Integer progresoActual;

  @Column(name = "completada", nullable = false)
  private boolean completada;

  @Column(name = "fecha_completada")
  private LocalDate fechaCompletada;

  @Embedded private InsigniaEmbeddable insignia;
}
