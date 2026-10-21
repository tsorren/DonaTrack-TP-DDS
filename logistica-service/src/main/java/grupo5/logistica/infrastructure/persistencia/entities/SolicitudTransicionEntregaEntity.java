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
@Table(name = "solicitud_transicion_entrega")
@Getter
@Setter
@NoArgsConstructor
public class SolicitudTransicionEntregaEntity {

  @Id
  @Column(name = "id_solicitud", nullable = false)
  private UUID idSolicitud;

  @Column(name = "id_entrega", nullable = false)
  private UUID idEntrega;

  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  @Column(name = "tipo_transicion", nullable = false, columnDefinition = "transicion_entrega")
  private TipoTransicionEntrega tipoTransicion;

  @Column(name = "actor", nullable = false, columnDefinition = "TEXT")
  private String actor;

  @Column(name = "ocurrio_en", nullable = false)
  private Instant ocurrioEn;

  @Column(name = "replanificable")
  private Boolean replanificable;

  @Column(name = "foto_recepcion_url", columnDefinition = "TEXT")
  private String fotoRecepcionUrl;

  @Column(name = "justificacion", columnDefinition = "TEXT")
  private String justificacion;
}
