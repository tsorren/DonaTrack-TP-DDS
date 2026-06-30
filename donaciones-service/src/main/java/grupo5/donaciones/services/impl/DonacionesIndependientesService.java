package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.comunicaciones.DonacionExitosaRequest;
import grupo5.donaciones.dto.comunicaciones.EventoDonacionRecibidaDTO;
import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.infrastructure.clients.IncentivosFeignClient;
import grupo5.donaciones.infrastructure.clients.NotificacionesFeignClient;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.services.IDonacionesIndependientesService;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DonacionesIndependientesService implements IDonacionesIndependientesService {

  private final IDonacionesIndependientesRepository repositorio;
  private final IncentivosFeignClient incentivosFeignClient;
  private final NotificacionesFeignClient notificacionesFeignClient;
  private final IDonacionesRepository donacionRepository;
  private final IDonantesRepository donantesRepository;
  private final grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository
      entidadesBeneficiariasRepository;
  private final grupo5.donaciones.services.mappers.DonacionIndependienteMapper
      donacionIndependienteMapper;

  public DonacionesIndependientesService(
      IDonacionesIndependientesRepository repositorio,
      IncentivosFeignClient incentivosFeignClient,
      NotificacionesFeignClient notificacionesFeignClient,
      IDonacionesRepository donacionRepository,
      IDonantesRepository donantesRepository,
      grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository
          entidadesBeneficiariasRepository,
      grupo5.donaciones.services.mappers.DonacionIndependienteMapper donacionIndependienteMapper) {
    this.repositorio = repositorio;
    this.incentivosFeignClient = incentivosFeignClient;
    this.notificacionesFeignClient = notificacionesFeignClient;
    this.donacionRepository = donacionRepository;
    this.donantesRepository = donantesRepository;
    this.entidadesBeneficiariasRepository = entidadesBeneficiariasRepository;
    this.donacionIndependienteMapper = donacionIndependienteMapper;
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
        UUID donacionOriginalId = donacion.getDonacionOriginalId();
        Donacion donacionOriginal =
            donacionRepository
                .findById(donacionOriginalId)
                .orElseThrow(() -> new RecursoNoEncontradoException(donacionOriginalId));
        UUID donanteId = donacionOriginal.getDonanteId();
        Donante donante =
            donantesRepository
                .findById(donanteId)
                .orElseThrow(() -> new RecursoNoEncontradoException(donanteId));
        UUID personaDonanteId = donante.personaId();
        UUID organizacionId = obtenerOrganizacionId(donacion);
        UUID idPersonaBeneficiaria = obtenerPersonaBeneficiariaId(donacion);
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
    return donacionIndependienteMapper.toDTO(donacion);
  }

  private UUID obtenerOrganizacionId(DonacionIndependiente donacion) {
    if (donacion.getAsignadaA() == null) {
      return null;
    }
    Necesidad necesidad = donacion.getAsignadaA().obtenerNecesidad();
    if (necesidad == null) {
      return null;
    }
    return necesidad.getEntidadId();
  }

  private UUID obtenerPersonaBeneficiariaId(DonacionIndependiente donacion) {
    if (donacion.getAsignadaA() == null) {
      return null;
    }
    Necesidad necesidad = donacion.getAsignadaA().obtenerNecesidad();
    if (necesidad == null || necesidad.getEntidadId() == null) {
      return null;
    }
    return entidadesBeneficiariasRepository
        .findById(necesidad.getEntidadId())
        .map(grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria::juridicaId)
        .orElse(null);
  }
}
