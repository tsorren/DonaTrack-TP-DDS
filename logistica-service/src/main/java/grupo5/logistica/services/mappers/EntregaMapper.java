package grupo5.logistica.services.mappers;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.dto.entregas.CambioEstadoEntregaResponseDTO;
import grupo5.logistica.dto.entregas.CrearEntregaRequestDTO;
import grupo5.logistica.dto.entregas.EntregaResponseDTO;
import grupo5.logistica.models.entities.entregas.CambioEstadoEntrega;
import grupo5.logistica.models.entities.entregas.Entrega;
import org.springframework.stereotype.Component;

@Component
public class EntregaMapper {

  private final DireccionMapper direccionMapper;

  public EntregaMapper(DireccionMapper direccionMapper) {
    this.direccionMapper = direccionMapper;
  }

  public Entrega toEntity(CrearEntregaRequestDTO dto) {
    if (dto == null) {
      return null;
    }

    validarMagnitudes(dto);
    return new Entrega(
        dto.idDonacion(),
        dto.idBeneficiaria(),
        direccionMapper.toEntity(dto.destino()),
        dto.pesoTotalKG(),
        dto.volumenTotalM3());
  }

  public EntregaResponseDTO toResponseDTO(Entrega entrega) {
    if (entrega == null) {
      return null;
    }

    return new EntregaResponseDTO(
        entrega.getId(),
        entrega.getIdRuta(),
        entrega.getIdDonacion(),
        entrega.getIdBeneficiaria(),
        direccionMapper.toResponseDTO(entrega.getDestino()),
        entrega.getEstadoActual(),
        entrega.getHoraSalida(),
        entrega.getHoraArribo(),
        entrega.getFotoRecepcionUrl(),
        entrega.getConfirmacionEntrega(),
        entrega.getPesoTotalKG(),
        entrega.getVolumenTotalM3(),
        entrega.getHistorialEstado().stream().map(this::toCambioEstadoResponseDTO).toList());
  }

  public CambioEstadoEntregaResponseDTO toCambioEstadoResponseDTO(CambioEstadoEntrega cambio) {
    if (cambio == null) {
      return null;
    }

    return new CambioEstadoEntregaResponseDTO(
        cambio.getEstadoAnterior(),
        cambio.getEstadoNuevo(),
        cambio.getTimeStamp(),
        cambio.getActor());
  }

  private void validarMagnitudes(CrearEntregaRequestDTO dto) {
    if (dto.pesoTotalKG() == null || dto.volumenTotalM3() == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
  }
}
