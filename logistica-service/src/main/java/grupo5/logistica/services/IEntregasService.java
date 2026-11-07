package grupo5.logistica.services;

import grupo5.logistica.dto.entregas.*;
import java.util.List;
import java.util.UUID;

public interface IEntregasService {
  EntregaResponseDTO crear(CrearEntregaRequestDTO dto);

  List<EntregaResponseDTO> listar();

  List<EntregaMapaPublicoResponseDTO> listarMapaPublico();

  EntregaResponseDTO obtenerPorId(UUID id);

  SeguimientoEntregaResponseDTO obtenerSeguimientoPorDonacion(UUID idDonacion);

  List<SeguimientoEntregaResponseDTO> listarSeguimientoPorBeneficiaria(UUID idBeneficiaria);

  EntregaResponseDTO cambiarEstado(UUID id, CambioEstadoEntregaRequestDTO dto);

  EntregaResponseDTO adjuntarFotoRecepcion(UUID id, AdjuntarFotoRecepcionRequestDTO dto);

  List<CambioEstadoEntregaResponseDTO> obtenerHistorial(UUID id);
}
