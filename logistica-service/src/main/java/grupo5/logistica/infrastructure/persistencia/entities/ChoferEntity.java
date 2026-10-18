package grupo5.logistica.infrastructure.persistencia.entities;

import grupo5.logistica.models.entities.choferes.EstadoChofer;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

@Entity
@Table(name = "chofer")
@Getter
@Setter
@NoArgsConstructor
public class ChoferEntity {

  @Id
  @Column(name = "id_chofer", nullable = false)
  private UUID idChofer;

  @Column(name = "nombre", nullable = false, columnDefinition = "TEXT")
  private String nombre;

  @Column(name = "apellido", nullable = false, columnDefinition = "TEXT")
  private String apellido;

  @Column(name = "licencia", nullable = false, columnDefinition = "TEXT")
  private String licencia;

  @Column(name = "telefono", nullable = false, columnDefinition = "TEXT")
  private String telefono;

  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  @Column(name = "estado_chofer", nullable = false, columnDefinition = "estado_chofer")
  private EstadoChofer estadoChofer;

  @Version
  @Column(name = "version", nullable = false)
  private Long version;

  @SuppressWarnings("squid:S1319")
  @OneToMany(mappedBy = "chofer", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @OrderBy("timestamp ASC")
  @Fetch(FetchMode.SUBSELECT)
  private List<CambioEstadoChoferEntity> historialEstados = new ArrayList<>();
}
