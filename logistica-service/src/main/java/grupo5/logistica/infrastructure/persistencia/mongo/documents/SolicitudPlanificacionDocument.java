package grupo5.logistica.infrastructure.persistencia.mongo.documents;

import grupo5.logistica.models.entities.solicitudes.EstadoSolicitud;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@SuppressWarnings("java:S120")
@Document(collection = "solicitudes_planificacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudPlanificacionDocument {

  @Id private UUID id;

  private LocalDate fecha;
  private EstadoSolicitud estado;
  private Integer cantidadDonaciones;
  private String callbackUrl;

  @Builder.Default private Integer intentosFallidos = 0;

  private String motivoError;

  @Version private Long version;

  @Builder.Default private Set<UUID> rutasGeneradas = new LinkedHashSet<>();
}
