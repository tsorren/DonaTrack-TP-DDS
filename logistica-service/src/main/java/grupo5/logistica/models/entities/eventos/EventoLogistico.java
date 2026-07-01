package grupo5.logistica.models.entities.eventos;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public class EventoLogistico implements AggregateRoot {
  private final UUID id;
  private final TipoEventoLogistico tipo;
  private final UUID rutaId;
  private final UUID entregaId;
  private final UUID idDonacion;
  private final UUID idBeneficiaria;
  private final LocalDateTime fechaCreacion;
  private boolean procesado;
  private LocalDateTime fechaProcesado;

  public EventoLogistico(
      TipoEventoLogistico tipo, UUID rutaId, UUID entregaId, UUID idDonacion, UUID idBeneficiaria) {
    validarTipo(tipo);
    validarReferencia(rutaId, entregaId);

    this.id = UUID.randomUUID();
    this.tipo = tipo;
    this.rutaId = rutaId;
    this.entregaId = entregaId;
    this.idDonacion = idDonacion;
    this.idBeneficiaria = idBeneficiaria;
    this.fechaCreacion = LocalDateTime.now(ZoneId.of("UTC"));
    this.procesado = false;
  }

  public void marcarProcesado() {
    this.procesado = true;
    this.fechaProcesado = LocalDateTime.now(ZoneId.of("UTC"));
  }

  private void validarTipo(TipoEventoLogistico tipo) {
    if (Objects.isNull(tipo)) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
  }

  private void validarReferencia(UUID rutaId, UUID entregaId) {
    if (Objects.isNull(rutaId) && Objects.isNull(entregaId)) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
  }
}
