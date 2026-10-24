package grupo5.logistica.infrastructure.persistencia.mongo.documents; // NOSONAR

import grupo5.logistica.models.entities.rutas.EstadoRuta;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@SuppressWarnings("java:S120")
@Document(collection = "rutas")
@CompoundIndex(name = "idx_ruta_camion_estado", def = "{'idCamion': 1, 'estado': 1}")
@CompoundIndex(name = "idx_ruta_chofer_estado", def = "{'idChofer': 1, 'estado': 1}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RutaDocument {

  @Id private UUID id;

  private UUID idChofer;
  private UUID idCamion;

  @Indexed private LocalDate fecha;

  private EstadoRuta estado;
  private Instant horaInicioReal;
  private Instant horaFinReal;

  @Version private Long version;

  @Builder.Default private List<ParadaSubdocument> paradas = new ArrayList<>();

  @Builder.Default private List<CambioEstadoSubdocument> historialEstado = new ArrayList<>();
}
