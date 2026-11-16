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
public class PosicionCamionSubdocument {

  private Double latitud;
  private Double longitud;
  private Instant registradaEn;
}
