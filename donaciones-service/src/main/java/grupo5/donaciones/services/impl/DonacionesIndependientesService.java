package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.comunicaciones.DonacionExitosaRequest;
import grupo5.donaciones.dto.comunicaciones.EventoDonacionRecibidaDTO;
import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.infrastructure.clients.IncentivosFeignClient;
import grupo5.donaciones.infrastructure.clients.NotificacionesFeignClient;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.services.IDonacionesIndependientesService;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DonacionesIndependientesService implements IDonacionesIndependientesService {

  private final IDonacionesIndependientesRepository repositorio;
  private final IncentivosFeignClient incentivosFeignClient;
  private final NotificacionesFeignClient notificacionesFeignClient;

  public DonacionesIndependientesService(
      IDonacionesIndependientesRepository repositorio,
      IncentivosFeignClient incentivosFeignClient,
      NotificacionesFeignClient notificacionesFeignClient) {
    this.repositorio = repositorio;
    this.incentivosFeignClient = incentivosFeignClient;
    this.notificacionesFeignClient = notificacionesFeignClient;
  }

  @Override
  public DonacionIndependienteResponseDTO cambiarEstado(
      UUID id, CambioEstadoDonacionIndependienteRequestDTO request, String actor) {

    DonacionIndependiente donacion =
        repositorio.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));

    // Ver si asignacion realizada va aca o solo por el algoritmo
    // Agregar url de foto a necesidad al confirmar entrega
    switch (request.estado()) {
      case TipoEstadoDonacion.ASIGNACION_REALIZADA -> donacion.asignar(actor, null);
      case TipoEstadoDonacion.VENCIDA -> donacion.vencer(actor);
      case TipoEstadoDonacion.EN_TRASLADO -> donacion.planificarRuta(actor);
      case TipoEstadoDonacion.LISTA_PARA_ENTREGAR -> donacion.iniciarRecorrido(actor);
      case TipoEstadoDonacion.ENTREGADA -> {
        donacion.confirmarEntrega(actor);
        UUID donanteId = donacion.getDonacionOriginal().getDonante().getId();
        UUID personaDonanteId =
            donacion.getDonacionOriginal().getDonante().getPersona() != null
                ? donacion.getDonacionOriginal().getDonante().getPersona().getId()
                : null;
        UUID organizacionId = null;
        UUID idPersonaBeneficiaria = null;
        if (donacion.getAsignadaA() != null) {
          Necesidad necesidad = donacion.getAsignadaA().obtenerNecesidad();
          if (necesidad != null && necesidad.getEntidad() != null) {
            organizacionId = necesidad.getEntidad().getId();
            if (necesidad.getEntidad().getJuridica() != null) {
              idPersonaBeneficiaria = necesidad.getEntidad().getJuridica().getId();
            }
          }
        }
        incentivosFeignClient.procesarDonacionExitosa(
            new DonacionExitosaRequest(donanteId, organizacionId));

        notificacionesFeignClient.enviarEvento(
            new EventoDonacionRecibidaDTO(
                personaDonanteId,
                LocalDateTime.now(),
                idPersonaBeneficiaria,
                donacion.getDescripcion()));
      }
      case TipoEstadoDonacion.ENTREGA_FALLIDA -> donacion.registrarFalla(
          request.justificacion(), actor);
      case TipoEstadoDonacion.EN_DEPOSITO -> donacion.retornar(actor);
      default -> throw new IllegalArgumentException("Estado inválido: " + request.estado());
    }

    repositorio.save(donacion);
    return toDTO(donacion);
  }

  private static DonacionIndependienteResponseDTO toDTO(DonacionIndependiente donacion) {
    return new DonacionIndependienteResponseDTO(
        donacion.getId(),
        donacion.getEstadoActual().getClass().getSimpleName(),
        donacion.getHistorial().stream()
            .map(c -> c.getEstadoNuevo().getClass().getSimpleName())
            .toList());
  }
}
