package grupo5.donaciones.infrastructure.persistencia.entities.donaciones;

import grupo5.donaciones.infrastructure.persistencia.entities.personas.DireccionEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Tabla compartida (P-2): varias donaciones apuntan al mismo depósito. */
@Entity
@Table(name = "deposito")
@Getter
@Setter
public class DepositoEntity {
  @Id private UUID id;

  @Column(name = "nombre", nullable = false)
  private String nombre;

  @ManyToOne(optional = false)
  @JoinColumn(name = "id_direccion", nullable = false)
  private DireccionEntity direccion;
}
