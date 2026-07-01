package grupo5.logistica.services;

import grupo5.logistica.dto.entregas.AdjuntarFotoRecepcionRequestDTO;
import grupo5.logistica.dto.entregas.CambioEstadoEntregaResponseDTO;
import grupo5.logistica.dto.entregas.ConfirmarRecepcionRequestDTO;
import grupo5.logistica.dto.entregas.CrearEntregaRequestDTO;
import grupo5.logistica.dto.entregas.EntregaResponseDTO;
import grupo5.logistica.dto.entregas.RegresarAlDepositoRequestDTO;
import grupo5.logistica.dto.entregas.ReportarNoRecepcionRequestDTO;
import java.util.List;
import java.util.UUID;

public interface IEntregasService {
  EntregaResponseDTO crear(CrearEntregaRequestDTO dto);

  List<EntregaResponseDTO> listar();

  EntregaResponseDTO obtenerPorId(UUID id);

  EntregaResponseDTO confirmarRecepcion(UUID id, ConfirmarRecepcionRequestDTO dto);

  EntregaResponseDTO adjuntarFotoRecepcion(UUID id, AdjuntarFotoRecepcionRequestDTO dto);

  EntregaResponseDTO reportarNoRecepcion(UUID id, ReportarNoRecepcionRequestDTO dto);

  EntregaResponseDTO regresarAlDeposito(UUID id, RegresarAlDepositoRequestDTO dto);

  List<CambioEstadoEntregaResponseDTO> obtenerHistorial(UUID id);
}
