package grupo5.donaciones.infrastructure.persistencia.entities.personas;

import grupo5.donaciones.models.entities.personas.Genero;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "humana")
@DiscriminatorValue("HUMANA")
@Getter
@Setter
public class HumanaEntity extends PersonaEntity {
  @Column(name = "nombre", nullable = false)
  private String nombre;

  @Column(name = "apellido", nullable = false)
  private String apellido;

  @Enumerated(EnumType.STRING)
  @Column(name = "genero", length = 20)
  private Genero genero;

  @Column(name = "fecha_nacimiento")
  private LocalDate fechaNacimiento;

  // Una sola columna: una humana representa a una sola jurídica (D-14)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "representante_de")
  private JuridicaEntity representanteDe;
}
