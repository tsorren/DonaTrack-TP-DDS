package grupo5.logistica.infrastructure.persistencia.mongo.documents; // NOSONAR

import grupo5.logistica.models.entities.entregas.EstadoEntrega;
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
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@SuppressWarnings("java:S120")
@Document(collection = "entregas")
// Va acá y no en DireccionSubdocument: ese subdocumento también se embebe en rutas.paradas.
@CompoundIndex(name = "idx_entrega_destino_ubicacion", def = "{'destino.ubicacion': '2dsphere'}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntregaDocument {

  @Id private UUID id;

  private AsignacionSubdocument asignacion;

  private DireccionSubdocument destino;

  @Indexed(unique = true)
  private UUID idDonacion;

  @Indexed private UUID idBeneficiaria;

  @Indexed private EstadoEntrega estado;

  private Instant horaArribo;
  private Instant horaSalida;
  @Builder.Default private List<FotoRecepcionSubdocument> fotosRecepcion = new ArrayList<>();
  private Double volumenTotalM3;
  private Double pesoTotalKg;

  @Version private Long version;

  @Builder.Default private List<CambioEstadoSubdocument> historialEstado = new ArrayList<>();
}
