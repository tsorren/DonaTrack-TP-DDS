package grupo5.logistica.infrastructure.persistencia.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

@Entity
@Table(name = "evento_entrega")
@Getter
@Setter
@NoArgsConstructor
public class EventoEntregaEntity {

  @Id
  @Column(name = "id_evento_entrega", nullable = false)
  private UUID idEventoEntrega;

  @Column(name = "id_entrega", nullable = false)
  private UUID idEntrega;

  @Column(name = "id_donacion", nullable = false)
  private UUID idDonacion;

  @Column(name = "id_ruta")
  private UUID idRuta;

  @Column(name = "ocurrio_en", nullable = false)
  private Instant ocurrioEn;

  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  @Column(name = "tipo", nullable = false, columnDefinition = "tipo_evento_entrega")
  private TipoEventoEntrega tipo;

  @Column(name = "justificacion", columnDefinition = "TEXT")
  private String justificacion;

  @Column(name = "replanificable")
  private Boolean replanificable;
}
