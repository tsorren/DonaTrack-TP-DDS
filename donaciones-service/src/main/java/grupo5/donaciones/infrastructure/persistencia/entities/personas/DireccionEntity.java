package grupo5.donaciones.infrastructure.persistencia.entities.personas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "direccion")
@Getter
@Setter
public class DireccionEntity {
  @Id private UUID id;

  @Column(name = "calle", nullable = false)
  private String calle;

  @Column(name = "altura", nullable = false)
  private Integer altura;

  @Column(name = "piso")
  private Integer piso;

  @Column(name = "departamento")
  private String departamento;

  @Column(name = "codigo_postal", nullable = false)
  private String codigoPostal;

  @Column(name = "localidad", nullable = false)
  private String localidad;

  @Column(name = "provincia", nullable = false)
  private String provincia;

  @Column(name = "pais", nullable = false)
  private String pais;
}
