package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.comunicaciones.DonacionExitosaRequest;
import grupo5.donaciones.dto.comunicaciones.EventoDonacionRecibidaDTO;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaFallidaDTO;
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
      case TipoEstadoDonacion.LISTA_PARA_ENTREGAR -> donacion.planificarRuta(actor);

      case TipoEstadoDonacion.EN_TRASLADO -> {
        donacion.iniciarRecorrido(actor);

        DatosDestino destino = obtenerDatosDestino(donacion);
        notificacionesFeignClient.enviarEvento(
            new EventoDonacionRecibidaDTO(
                obtenerIdPersonaDonante(donacion),
                LocalDateTime.now(),
                destino.idPersonaBeneficiaria(),
                donacion.getDescripcion(),
                request.urlMapa()));
      }

      case TipoEstadoDonacion.ENTREGADA -> {
        donacion.confirmarEntrega(actor);
        UUID donanteId = donacion.getDonacionOriginal().getDonante().getId();
        DatosDestino destino = obtenerDatosDestino(donacion);

        incentivosFeignClient.procesarDonacionExitosa(
            new DonacionExitosaRequest(donanteId, destino.organizacionId()));

        notificacionesFeignClient.enviarEvento(
            new EventoDonacionRecibidaDTO(
                obtenerIdPersonaDonante(donacion),
                LocalDateTime.now(),
                destino.idPersonaBeneficiaria(),
                donacion.getDescripcion(),
                request.patenteCamion()));
      }

      case TipoEstadoDonacion.ENTREGA_FALLIDA -> {
        donacion.registrarFalla(request.justificacion(), actor);

        DatosDestino destino = obtenerDatosDestino(donacion);
        notificacionesFeignClient.enviarEvento(
            new EventoEntregaFallidaDTO(
                obtenerIdPersonaDonante(donacion),
                LocalDateTime.now(),
                destino.idPersonaBeneficiaria(),
                donacion.getDescripcion(),
                null, // TODO: resolver persona adminsitradora
                request.justificacion(),
                request.replanificable() // TODO: resolver logica de replanificacion
                ));
      }

      case TipoEstadoDonacion.EN_DEPOSITO -> donacion.retornar(actor);
      default -> throw new IllegalArgumentException("Estado inválido: " + request.estado());
    }

    repositorio.save(donacion);
    return toDTO(donacion);
  }

  private UUID obtenerIdPersonaDonante(DonacionIndependiente donacion) {
    return donacion.getDonacionOriginal().getDonante().getPersona() != null
        ? donacion.getDonacionOriginal().getDonante().getPersona().getId()
        : null;
  }

  private record DatosDestino(UUID organizacionId, UUID idPersonaBeneficiaria) {}

  private DatosDestino obtenerDatosDestino(DonacionIndependiente donacion) {
    if (donacion.getAsignadaA() == null) return new DatosDestino(null, null);
    Necesidad necesidad = donacion.getAsignadaA().obtenerNecesidad();
    if (necesidad == null || necesidad.getEntidad() == null) return new DatosDestino(null, null);
    UUID organizacionId = necesidad.getEntidad().getId();
    UUID idPersonaBeneficiaria =
        necesidad.getEntidad().getJuridica() != null
            ? necesidad.getEntidad().getJuridica().getId()
            : null;
    return new DatosDestino(organizacionId, idPersonaBeneficiaria);
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
