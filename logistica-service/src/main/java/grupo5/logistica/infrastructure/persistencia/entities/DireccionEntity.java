package grupo5.logistica.infrastructure.persistencia.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "direccion")
@Getter
@Setter
@NoArgsConstructor
public class DireccionEntity {

  @Id
  @Column(name = "id_direccion", nullable = false)
  private UUID idDireccion;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "id_localidad", nullable = false)
  private LocalidadEntity localidad;

  @Column(name = "calle", nullable = false, columnDefinition = "TEXT")
  private String calle;

  @Column(name = "altura", nullable = false)
  private Integer altura;

  @Column(name = "piso")
  private Short piso;

  @Column(name = "departamento", columnDefinition = "TEXT")
  private String departamento;

  @Column(name = "codigo_postal", nullable = false, columnDefinition = "TEXT")
  private String codigoPostal;
}
