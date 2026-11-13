package grupo5.logistica.services;

import grupo5.logistica.dto.entregas.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IEntregasService {
  EntregaResponseDTO crear(CrearEntregaRequestDTO dto);

  List<EntregaResponseDTO> listar();

  /**
   * El mapa público (RF-02, R11-19): las entregadas con coordenadas de {@code desde} a {@code
   * hasta} (días inclusive, en hora de Argentina), de la más reciente a la más vieja y como mucho
   * {@code limite}, con cuántas hay sin el tope. Sin {@code hasta}, hoy; sin {@code desde}, {@code
   * hasta} menos 30 días; sin {@code limite}, 200 (de 1 a 500).
   */
  EntregasDelMapaPublico listarMapaPublico(LocalDate desde, LocalDate hasta, Integer limite);

  EntregaResponseDTO obtenerPorId(UUID id);

  SeguimientoEntregaResponseDTO obtenerSeguimientoPorDonacion(UUID idDonacion);

  /**
   * Una página de las entregas de la organización (R11-43), en cualquier estado salvo que se pidan
   * estados, con las donaciones pedidas; cada página se arma con una lectura de rutas, una de
   * camiones y una de las entregas de esas rutas.
   */
  SeguimientosDeEntregas listarSeguimientoPorBeneficiaria(
      UUID idBeneficiaria, ConsultaSeguimiento consulta);

  /**
   * Una página de las entregas de un donante (RF-09, HB-16, R11-43): sin estados pedidos, las
   * activas (pendientes, en traslado o en la puerta).
   */
  SeguimientosDeEntregas listarSeguimientoPorDonante(UUID donanteId, ConsultaSeguimiento consulta);

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
