package grupo5.logistica.infrastructure.persistencia.mongo.documents; // NOSONAR

import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;

@SuppressWarnings("java:S120")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsignacionSubdocument {

  @Indexed private UUID idRuta;

  private LocalDate fecha;
  private UUID idCamion;
  private String patente;
}
