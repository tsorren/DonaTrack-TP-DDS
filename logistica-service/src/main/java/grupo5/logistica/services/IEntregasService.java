package grupo5.logistica.services;

import grupo5.logistica.dto.entregas.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IEntregasService {
  EntregaResponseDTO crear(CrearEntregaRequestDTO dto);

  List<EntregaResponseDTO> listar();

  List<EntregaMapaPublicoResponseDTO> listarMapaPublico();

  EntregaResponseDTO obtenerPorId(UUID id);

  SeguimientoEntregaResponseDTO obtenerSeguimientoPorDonacion(UUID idDonacion);

  List<SeguimientoEntregaResponseDTO> listarSeguimientoPorBeneficiaria(UUID idBeneficiaria);

  /** Entregas activas (pendientes, en traslado o en la puerta) de un donante (RF-09, HB-16). */
  List<SeguimientoEntregaResponseDTO> listarSeguimientoPorDonante(UUID donanteId);

  EntregaResponseDTO cambiarEstado(UUID id, CambioEstadoEntregaRequestDTO dto);

  EntregaResponseDTO adjuntarFotoRecepcion(UUID id, AdjuntarFotoRecepcionRequestDTO dto);

  List<CambioEstadoEntregaResponseDTO> obtenerHistorial(UUID id);

  void cancelarPorDonacion(UUID idDonacion, String motivo);

  /**
   * El camión llegó al destino (HB-22): la entrega pasa a {@code EN_PUERTA}. Idempotente si ya
   * estaba ahí. La llaman el {@code PATCH} de estado y el planificador de posiciones.
   */
  EntregaResponseDTO marcarEnPuerta(UUID id, String actor);

  /** Beneficiaria de la entrega, para la regla de pertenencia; vacío si la entrega no existe. */
  Optional<UUID> beneficiariaDe(UUID id);

  /** Donante de la entrega de la donación; vacío si no hay entrega o no se conoce su donante. */
  Optional<UUID> donanteDeDonacion(UUID idDonacion);
}
