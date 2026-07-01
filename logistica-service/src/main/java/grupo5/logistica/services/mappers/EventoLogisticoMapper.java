package grupo5.logistica.services.mappers;

import grupo5.logistica.dto.eventos.EventoLogisticoResponseDTO;
import grupo5.logistica.models.entities.eventos.EventoLogistico;
import org.springframework.stereotype.Component;

@Component
public class EventoLogisticoMapper {

  public EventoLogisticoResponseDTO toResponseDTO(EventoLogistico evento) {
    if (evento == null) {
      return null;
    }

    return new EventoLogisticoResponseDTO(
        evento.getId(),
        evento.getTipo(),
        evento.getRutaId(),
        evento.getEntregaId(),
        evento.getIdDonacion(),
        evento.getIdBeneficiaria(),
        evento.getFechaCreacion(),
        evento.isProcesado(),
        evento.getFechaProcesado());
  }
}
