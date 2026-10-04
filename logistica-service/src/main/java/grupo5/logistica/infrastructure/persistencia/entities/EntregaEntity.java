package grupo5.logistica.infrastructure.persistencia.entities;

import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
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
@Table(name = "entrega")
@Getter
@Setter
@NoArgsConstructor
public class EntregaEntity {

  @Id
  @Column(name = "id_entrega", nullable = false)
  private UUID idEntrega;

  @Column(name = "id_ruta")
  private UUID idRuta;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "id_direccion", nullable = false)
  private DireccionEntity direccion;

  @Column(name = "id_donacion", nullable = false, unique = true)
  private UUID idDonacion;

  @Column(name = "id_beneficiario", nullable = false)
  private UUID idBeneficiario;

  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  @Column(name = "estado", nullable = false, columnDefinition = "estado_entrega")
  private EstadoEntrega estado;

  @Column(name = "hora_arribo")
  private Instant horaArribo;

  @Column(name = "hora_salida")
  private Instant horaSalida;

  @Column(name = "foto_recepcion_url", columnDefinition = "TEXT")
  private String fotoRecepcionUrl;

  @Column(name = "volumen_total_m3", nullable = false)
  private Double volumenTotalM3;

  @Column(name = "peso_total_kg", nullable = false)
  private Double pesoTotalKg;

  @Version
  @Column(name = "version", nullable = false)
  private Long version;

  @SuppressWarnings("squid:S1319")
  @OneToMany(mappedBy = "entrega", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @OrderBy("timestamp ASC")
  @Fetch(FetchMode.SUBSELECT)
  private List<CambioEstadoEntregaEntity> historialEstado = new ArrayList<>();
}
