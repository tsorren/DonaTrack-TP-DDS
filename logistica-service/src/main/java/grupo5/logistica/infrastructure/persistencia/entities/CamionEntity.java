package grupo5.logistica.infrastructure.persistencia.entities;

import grupo5.logistica.models.entities.camiones.EstadoCamion;
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
@Table(name = "camion")
@Getter
@Setter
@NoArgsConstructor
public class CamionEntity {

  @Id
  @Column(name = "id_camion", nullable = false)
  private UUID idCamion;

  @Column(name = "patente", nullable = false, unique = true, columnDefinition = "TEXT")
  private String patente;

  @Column(name = "capacidad_volumen", nullable = false)
  private Double capacidadVolumen;

  @Column(name = "capacidad_peso", nullable = false)
  private Double capacidadPeso;

  @Column(name = "altura", nullable = false)
  private Double altura;

  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  @Column(name = "estado_camion", nullable = false, columnDefinition = "estado_camion")
  private EstadoCamion estadoCamion;

  @Version
  @Column(name = "version", nullable = false)
  private Long version;

  @SuppressWarnings("squid:S1319")
  @OneToMany(mappedBy = "camion", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @OrderBy("timestamp ASC")
  @Fetch(FetchMode.SUBSELECT)
  private List<CambioEstadoCamionEntity> historialEstado = new ArrayList<>();
}
