package grupo5.logistica.infrastructure.persistencia.mongo.documents; // NOSONAR

import grupo5.logistica.infrastructure.persistencia.jpa.entities.TipoEventoEntrega;
import java.time.Instant;
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
@Document(collection = "eventos_entrega")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventoEntregaDocument {

  @Id private UUID id;

  @Indexed private UUID idEntrega;

  private UUID idDonacion;
  private UUID idRuta;
  private Instant ocurrioEn;
  private TipoEventoEntrega tipo;
  private String justificacion;
  private Boolean replanificable;
}
