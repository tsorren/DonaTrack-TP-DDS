package grupo5.logistica.infrastructure.persistencia.mongo.documents; // NOSONAR

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@SuppressWarnings("java:S120")
@Document(collection = "solicitudes_transicion_entrega")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudTransicionEntregaDocument {

  @Id private UUID id;

  @Indexed private UUID idEntrega;

  private TipoTransicionEntrega tipoTransicion;
  private String actor;
  private Instant ocurrioEn;
  private Boolean replanificable;
  @Builder.Default private List<String> fotosRecepcion = new ArrayList<>();
  private String justificacion;
}
