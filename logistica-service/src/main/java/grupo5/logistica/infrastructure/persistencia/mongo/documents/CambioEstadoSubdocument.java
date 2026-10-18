package grupo5.logistica.infrastructure.persistencia.mongo.documents;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SuppressWarnings("java:S120")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CambioEstadoSubdocument {

  private String estadoAnterior;
  private String estadoNuevo;
  private Instant timestamp;
  private String actor;
}
