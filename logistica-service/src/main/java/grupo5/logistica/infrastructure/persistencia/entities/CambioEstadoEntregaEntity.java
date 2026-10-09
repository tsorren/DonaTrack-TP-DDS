package grupo5.logistica.infrastructure.persistencia.entities;

import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

@Entity
@Table(name = "cambio_estado_entrega")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CambioEstadoEntregaEntity {

  @Id
  @EqualsAndHashCode.Include
  @Column(name = "id_cambio_estado_entrega", nullable = false)
  private UUID idCambioEstadoEntrega;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_entrega", nullable = false)
  private EntregaEntity entrega;

  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  @Column(name = "estado_anterior", columnDefinition = "estado_entrega")
  private EstadoEntrega estadoAnterior;

  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  @Column(name = "estado_nuevo", nullable = false, columnDefinition = "estado_entrega")
  private EstadoEntrega estadoNuevo;

  @Column(name = "timestamp", nullable = false)
  private Instant timestamp;

  @Column(name = "actor", nullable = false, columnDefinition = "TEXT")
  private String actor;
}
