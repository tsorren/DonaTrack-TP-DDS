package grupo5.logistica.infrastructure.persistencia.mongo.documents; // NOSONAR

import java.util.UUID;
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
public class ParadaSubdocument {

  private Integer orden;
  private UUID idEntrega;
  private UUID idDonacion;
  private DireccionSubdocument destino;

  public boolean estaCompleta() {
    return idDonacion != null && destino != null;
  }

  public void completarCon(EntregaDocument entrega) {
    this.idDonacion = entrega.getIdDonacion();
    this.destino = entrega.getDestino();
  }
}
