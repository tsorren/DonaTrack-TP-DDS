package grupo5.logistica.infrastructure.persistencia.mongo.documents; // NOSONAR

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
public class FotoRecepcionSubdocument {

  /** Clave del objeto en MinIO. */
  private String clave;

  private Instant subidaEn;
}
