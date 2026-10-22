package grupo5.logistica.services.mappers;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.dto.entregas.CambioEstadoEntregaResponseDTO;
import grupo5.logistica.dto.entregas.ConfirmarRecepcionRequestDTO;
import grupo5.logistica.dto.entregas.CrearEntregaRequestDTO;
import grupo5.logistica.dto.entregas.EntregaMapaPublicoResponseDTO;
import grupo5.logistica.dto.entregas.EntregaResponseDTO;
import grupo5.logistica.dto.entregas.RegresarAlDepositoRequestDTO;
import grupo5.logistica.dto.entregas.ReportarNoRecepcionRequestDTO;
import grupo5.logistica.models.entities.entregas.CambioEstadoEntrega;
import grupo5.logistica.models.entities.entregas.ConfirmacionRecepcion;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.entities.entregas.NoRecepcion;
import grupo5.logistica.models.entities.entregas.RegresoDeposito;
import grupo5.logistica.models.entities.rutas.direccion.CoordenadasGeograficas;
import grupo5.logistica.models.entities.rutas.direccion.Direccion;
import java.time.LocalDateTime;
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
        entrega.getFotosRecepcion(),
        entrega.getPesoTotalKG(),
        entrega.getVolumenTotalM3(),
        entrega.getHistorialEstado().stream().map(this::toCambioEstadoResponseDTO).toList());
  }

  public EntregaMapaPublicoResponseDTO toMapaPublicoResponseDTO(Entrega entrega) {
    if (entrega == null
        || entrega.getDestino() == null
        || !entrega.getDestino().tieneCoordenadas()) {
      return null;
    }

    Direccion destino = entrega.getDestino();
    CoordenadasGeograficas coords = destino.coordenadas();
    LocalDateTime fechaEntrega = resolverFechaEntrega(entrega);

    return new EntregaMapaPublicoResponseDTO(
        entrega.getId(),
        coords.latitud(),
        coords.longitud(),
        destino.localidad().nombre(),
        destino.localidad().provincia().nombre(),
        destino.localidad().provincia().pais().nombre(),
        fechaEntrega,
        entrega.getPesoTotalKG(),
        entrega.getVolumenTotalM3());
  }

  public ConfirmacionRecepcion toSolicitud(Entrega entrega, ConfirmarRecepcionRequestDTO dto) {
    return new ConfirmacionRecepcion(entrega, dto.actor(), dto.obtenerFotosRecepcion());
  }

  public NoRecepcion toSolicitud(Entrega entrega, ReportarNoRecepcionRequestDTO dto) {
    boolean replanificable = dto.replanificable() == null || dto.replanificable();
    return new NoRecepcion(entrega, dto.actor(), dto.justificacion(), replanificable);
  }

  public RegresoDeposito toSolicitud(Entrega entrega, RegresarAlDepositoRequestDTO dto) {
    return new RegresoDeposito(entrega, dto.actor());
  }

  public CambioEstadoEntregaResponseDTO toCambioEstadoResponseDTO(CambioEstadoEntrega cambio) {
    if (cambio == null) {
      return null;
    }

    return new CambioEstadoEntregaResponseDTO(
        cambio.estadoAnterior(), cambio.estadoNuevo(), cambio.timeStamp(), cambio.actor());
  }

  private static LocalDateTime resolverFechaEntrega(Entrega entrega) {
    if (entrega.getHoraArribo() != null) {
      return entrega.getHoraArribo();
    }
    return entrega.getHistorialEstado().stream()
        .filter(c -> c.estadoNuevo() == EstadoEntrega.ENTREGADA)
        .map(CambioEstadoEntrega::timeStamp)
        .reduce((primero, ultimo) -> ultimo)
        .orElse(null);
  }

  private static void validarMagnitudes(CrearEntregaRequestDTO dto) {
    if (dto.pesoTotalKG() == null || dto.volumenTotalM3() == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
  }
}
