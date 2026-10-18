package grupo5.logistica.infrastructure.persistencia.mongo.documents;

import grupo5.logistica.models.entities.choferes.EstadoChofer;
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
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@SuppressWarnings("java:S120")
@Document(collection = "choferes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChoferDocument {

  @Id private UUID id;

  private String nombre;
  private String apellido;
  private String licencia;
  private String telefono;

  @Indexed private EstadoChofer estadoChofer;

  @Version private Long version;

  @Builder.Default private List<CambioEstadoSubdocument> historialEstados = new ArrayList<>();
}
