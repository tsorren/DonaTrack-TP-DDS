package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.comunicaciones.DonacionExitosaRequest;
import grupo5.donaciones.dto.comunicaciones.EventoDonacionRecibidaDTO;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaFallidaDTO;
import grupo5.donaciones.dto.comunicaciones.EventoRutaIniciadaDTO;
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
import grupo5.donaciones.services.IPersonasService;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
  private final IPersonasService personasService;

  public DonacionesIndependientesService(
      IDonacionesIndependientesRepository repositorio,
      IncentivosFeignClient incentivosFeignClient,
      NotificacionesFeignClient notificacionesFeignClient,
      IDonacionesRepository donacionRepository,
      IDonantesRepository donantesRepository,
      grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository
          entidadesBeneficiariasRepository,
      grupo5.donaciones.services.mappers.DonacionIndependienteMapper donacionIndependienteMapper,
      IPersonasService personasService) {
    this.repositorio = repositorio;
    this.incentivosFeignClient = incentivosFeignClient;
    this.notificacionesFeignClient = notificacionesFeignClient;
    this.donacionRepository = donacionRepository;
    this.donantesRepository = donantesRepository;
    this.entidadesBeneficiariasRepository = entidadesBeneficiariasRepository;
    this.donacionIndependienteMapper = donacionIndependienteMapper;
    this.personasService = personasService;
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
      case TipoEstadoDonacion.EN_TRASLADO -> procesarDonacionEnTraslado(
          actor, donacion, request.urlMapa());
      case TipoEstadoDonacion.ENTREGADA -> procesarDonacionEntregada(
          actor, donacion, request.patenteCamion());
      case TipoEstadoDonacion.ENTREGA_FALLIDA -> procesarEntregaFallida(
          actor, donacion, request.justificacion(), request.replanificable());
      case TipoEstadoDonacion.EN_DEPOSITO -> donacion.retornar(actor);
      default -> throw new IllegalArgumentException("Estado inválido: " + request.estado());
    }

    repositorio.save(donacion);
    return donacionIndependienteMapper.toDTO(donacion);
  }

  private void procesarDonacionEnTraslado(
      String actor, DonacionIndependiente donacion, String urlMapa) {
    donacion.iniciarRecorrido(actor);

    UUID donanteId = obtenerDonanteId(donacion);
    UUID personaDonanteId = obtenerPersonaDonanteId(donanteId);
    UUID idPersonaBeneficiaria = obtenerPersonaBeneficiariaId(donacion);

    notificacionesFeignClient.enviarEvento(
        new EventoRutaIniciadaDTO(
            personaDonanteId,
            LocalDateTime.now(ZoneId.systemDefault()),
            idPersonaBeneficiaria,
            donacion.getDescripcion(),
            urlMapa));
  }

  private void procesarDonacionEntregada(
      String actor, DonacionIndependiente donacion, String patenteCamion) {
    donacion.confirmarEntrega(actor);

    UUID donanteId = obtenerDonanteId(donacion);
    UUID personaDonanteId = obtenerPersonaDonanteId(donanteId);
    UUID organizacionId = obtenerOrganizacionId(donacion);
    UUID idPersonaBeneficiaria = obtenerPersonaBeneficiariaId(donacion);

    incentivosFeignClient.procesarDonacionExitosa(
        new DonacionExitosaRequest(donanteId, organizacionId));

    notificacionesFeignClient.enviarEvento(
        new EventoDonacionRecibidaDTO(
            personaDonanteId,
            LocalDateTime.now(ZoneId.systemDefault()),
            idPersonaBeneficiaria,
            donacion.getDescripcion(),
            patenteCamion));
  }

  private void procesarEntregaFallida(
      String actor, DonacionIndependiente donacion, String justificacion, Boolean replanificable) {
    donacion.registrarFalla(justificacion, actor);

    if (Boolean.TRUE.equals(replanificable)) {
      donacion.replanificar(actor);
    }

    UUID donanteId = obtenerDonanteId(donacion);
    UUID personaDonanteId = obtenerPersonaDonanteId(donanteId);
    UUID idPersonaBeneficiaria = obtenerPersonaBeneficiariaId(donacion);
    UUID idPersonaAdmin = personasService.obtenerIdPersonaAdministradora();

    notificacionesFeignClient.enviarEvento(
        new EventoEntregaFallidaDTO(
            personaDonanteId,
            LocalDateTime.now(ZoneId.systemDefault()),
            idPersonaBeneficiaria,
            donacion.getDescripcion(),
            idPersonaAdmin,
            justificacion,
            replanificable));
  }

  private UUID obtenerDonanteId(DonacionIndependiente donacion) {
    UUID donacionOriginalId = donacion.getDonacionOriginalId();
    Donacion donacionOriginal =
        donacionRepository
            .findById(donacionOriginalId)
            .orElseThrow(() -> new RecursoNoEncontradoException(donacionOriginalId));
    return donacionOriginal.getDonanteId();
  }

  private UUID obtenerPersonaDonanteId(UUID donanteId) {
    Donante donante =
        donantesRepository
            .findById(donanteId)
            .orElseThrow(() -> new RecursoNoEncontradoException(donanteId));
    return donante.personaId();
  }

  private static UUID obtenerOrganizacionId(DonacionIndependiente donacion) {
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
