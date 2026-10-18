package grupo5.logistica.infrastructure.persistencia.mongo.documents;

import grupo5.logistica.models.entities.camiones.EstadoCamion;
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
@Document(collection = "camiones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CamionDocument {

  @Id private UUID id;

  @Indexed(unique = true)
  private String patente;

  private Double capacidadVolumen;
  private Double capacidadPeso;
  private Double altura;

  @Indexed private EstadoCamion estadoCamion;

  @Version private Long version;

  @Builder.Default private List<CambioEstadoSubdocument> historialEstado = new ArrayList<>();
}
