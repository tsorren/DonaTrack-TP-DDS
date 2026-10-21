package grupo5.logistica.infrastructure.persistencia.entities;

import grupo5.logistica.models.entities.solicitudes.EstadoSolicitud;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

@Entity
@Table(name = "solicitud_planificacion")
@Getter
@Setter
@NoArgsConstructor
public class SolicitudPlanificacionEntity {

  @Id
  @Column(name = "id_solicitud_planificacion", nullable = false)
  private UUID idSolicitudPlanificacion;

  @Column(name = "fecha", nullable = false)
  private LocalDate fecha;

  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  @Column(name = "estado", nullable = false, columnDefinition = "estado_solicitud")
  private EstadoSolicitud estado;

  @Column(name = "cantidad_donaciones", nullable = false)
  private Integer cantidadDonaciones;

  @Column(name = "callback_url", nullable = false, columnDefinition = "TEXT")
  private String callbackUrl;

  @Column(name = "intentos_fallidos", nullable = false)
  private Integer intentosFallidos = 0;

  @Column(name = "motivo_error", columnDefinition = "TEXT")
  private String motivoError;

  @Version
  @Column(name = "version", nullable = false)
  private Long version;

  @SuppressWarnings("squid:S1319")
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "solicitud_planificacion_ruta",
      joinColumns = @JoinColumn(name = "id_solicitud_planificacion", nullable = false))
  @Column(name = "id_ruta", nullable = false)
  @Fetch(FetchMode.SUBSELECT)
  private Set<UUID> rutasGeneradas = new LinkedHashSet<>();
}
