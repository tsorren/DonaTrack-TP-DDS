package grupo5.logistica.infrastructure.persistencia.entities;

import grupo5.logistica.models.entities.rutas.EstadoRuta;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
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
@Table(name = "ruta")
@Getter
@Setter
@NoArgsConstructor
public class RutaEntity {

  @Id
  @Column(name = "id_ruta", nullable = false)
  private UUID idRuta;

  @Column(name = "id_chofer", nullable = false)
  private UUID idChofer;

  @Column(name = "id_camion", nullable = false)
  private UUID idCamion;

  @Column(name = "fecha", nullable = false)
  private LocalDate fecha;

  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  @Column(name = "estado", nullable = false, columnDefinition = "estado_ruta")
  private EstadoRuta estado;

  @Column(name = "hora_inicio_real")
  private Instant horaInicioReal;

  @Column(name = "hora_fin_real")
  private Instant horaFinReal;

  @Version
  @Column(name = "version", nullable = false)
  private Long version;

  @SuppressWarnings("squid:S1319")
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "parada_ruta",
      joinColumns = @JoinColumn(name = "id_ruta", nullable = false))
  @OrderColumn(name = "orden_visita")
  @Column(name = "id_entrega", nullable = false)
  @Fetch(FetchMode.SUBSELECT)
  private List<UUID> entregaIds = new ArrayList<>();

  @SuppressWarnings("squid:S1319")
  @OneToMany(mappedBy = "ruta", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @OrderBy("timestamp ASC")
  @Fetch(FetchMode.SUBSELECT)
  private List<CambioEstadoRutaEntity> historialEstado = new ArrayList<>();
}
